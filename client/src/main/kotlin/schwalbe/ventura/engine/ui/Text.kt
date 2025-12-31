
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Texture
import schwalbe.ventura.engine.gfx.fromBufferedImage
import org.joml.*
import java.awt.Color
import java.awt.Font as AwtFont
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.LineBreakMeasurer
import java.awt.font.TextAttribute
import java.awt.font.TextHitInfo
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import java.text.AttributedCharacterIterator
import java.text.AttributedString
import kotlin.math.ceil
import kotlin.math.roundToInt

class Text : UiElement(), Colored {
    
    enum class Alignment(val xPosOf: (max: Float, line: Float) -> Float) {
        LEFT({ max, line -> 0f }),
        CENTER({ max, line -> (max - line) / 2f }),
        RIGHT({ max, line -> max - line })
    }
    
    companion object {
        
        private fun configureGraphics2D(g: Graphics2D): Graphics2D {
            g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            )
            g.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON
            )
            return g
        }
        
        private val baseImage: BufferedImage = BufferedImage(
            1, 1, BufferedImage.TYPE_INT_ARGB_PRE
        )
        private val baseGraphics: Graphics2D
            = this.configureGraphics2D(this.baseImage.createGraphics())
        private val baseFrc: FontRenderContext
            = this.baseGraphics.fontRenderContext

        private fun splitLines(
            paragraph: String, awtFont: AwtFont, widthLimit: Float
        ): SplitLines {
            val text = AttributedString(paragraph.ifEmpty { " " }) 
            text.addAttribute(TextAttribute.FONT, awtFont)
            val chars: AttributedCharacterIterator = text.iterator
            val lineBreaks = LineBreakMeasurer(chars, Text.baseFrc)
            val lines = mutableListOf<TextLayout>()
            var maxWidth = 0f
            var maxHeight = 0f
            while (lineBreaks.position < chars.endIndex) {
                val line: TextLayout = lineBreaks.nextLayout(widthLimit)
                lines.add(line)
                val width: Float = maxOf(
                    line.bounds.width.toFloat(),
                    minOf(line.advance, widthLimit)
                )
                maxWidth = maxOf(maxWidth, width)
                maxHeight += line.ascent + line.descent + line.leading
            }
            return SplitLines(
                lines, ceil(maxWidth).toInt(), ceil(maxHeight).toInt()
            )
        }

    }
    
    private data class SplitLines(
        val lines: List<TextLayout>,
        val maxWidth: Int,
        val maxHeight: Int
    )
    
    private data class RenderState(
        val awtFont: AwtFont,
        val lines: List<TextLayout>,
        val image: BufferedImage,
        val g: Graphics2D
    )
    
    
    private var renderIsDirty: Boolean = true
    
    var value: String = ""
        set(value) {
            field = value
            this.invalidateRender()
        }

    var font: Font? = null
        set(value) {
            field = value
            this.invalidateRender()
        }
    var fontSize: UiSize? = null
        set(value) {
            field = value
            this.invalidateRender()
        }
    var wrapText: Boolean = true
        set(value) {
            if (field != value) { this.invalidateRender() }
            field = value
        }
    var alignment: Alignment = Alignment.LEFT
        set(value) {
            if (field != value) { this.invalidateRender() }
            field = value
        }
    var lineHeight: Float = 1f
        set(value) {
            field = value
            this.invalidateRender()
        }
    override var color: Vector4fc? = null
        set(value) {
            field = if (value == null) { null }
                else { Vector4f(value) }
            this.invalidateRender()
        }
    
    override val children: List<UiElement> = listOf()
    
    private fun invalidateRender() {
        this.renderIsDirty = true
        this.invalidate()
    }
    
    private var lastPxWidth: Int = -1
    private var lastPxHeight: Int = -1
    private var renderState: RenderState? = null
    
    override fun updateLayout(context: UiElementContext) {
        val font: Font = this.font
            ?: context.global.defaultFont
        val fontSize: UiSize = this.fontSize
            ?: context.global.defaultFontSize
        val fontSizePx: Float = fontSize(context)
        val widthLimit: Float = if (!this.wrapText) { Float.POSITIVE_INFINITY }
            else { this.pxWidth.toFloat() }
        val awtFont: AwtFont = font.baseFont.deriveFont(fontSizePx)
        val paragraphs: List<SplitLines> = this.value.split("\n")
            .map { p -> Text.splitLines(p, awtFont, widthLimit) }
            .toList()
        val lines: List<TextLayout> = paragraphs.flatMap(SplitLines::lines)
        this.pxWidth = maxOf(
            this.pxWidth, paragraphs.maxOf { p -> p.maxWidth }
        )
        this.pxHeight = maxOf(
            this.pxHeight, paragraphs.sumOf { p -> p.maxHeight }
        )
        this.renderIsDirty = this.renderIsDirty
            || this.lastPxWidth != this.pxWidth
            || this.lastPxHeight != this.pxHeight
        if (!this.renderIsDirty) { return }
        this.lastPxWidth = this.pxWidth
        this.lastPxHeight = this.pxHeight
        val image: BufferedImage
        val g: Graphics2D
        val oldState: RenderState? = this.renderState
        val useOldImage: Boolean = oldState != null
            && oldState.image.width == this.pxWidth
            && oldState.image.height == this.pxHeight
        if (useOldImage) {
            image = oldState.image
            g = oldState.g
            g.background = Color(0, 0, 0, 0);
            g.clearRect(0, 0, image.width, image.height)
        } else {
            image = BufferedImage(
                this.pxWidth, this.pxHeight, BufferedImage.TYPE_INT_ARGB_PRE
            )
            g = Text.configureGraphics2D(image.createGraphics())
            g.translate(0, image.height)
            g.scale(1.0, -1.0)
        }
        this.renderState = RenderState(awtFont, lines, image, g)
    }
    
    fun charIdxOfPos(relPos: Vector2fc, lineIdx: Int): Int? {
        val line: TextLayout = this.renderState?.lines?.getOrNull(lineIdx)
            ?: return null
        val x: Float = relPos.x().coerceIn(0f, line.advance)
        val hit = line.hitTestChar(x, 0f)
        return hit.insertionIndex
    }
    
    fun posOfCharIdx(lineCharIdx: Int, lineIdx: Int): Vector2f? {
        val lines: List<TextLayout> = this.renderState?.lines ?: return null
        var caretY = 0f
        for ((lineI, line) in lines.withIndex()) {
            if (lineI != lineIdx) {
                val rawHeight: Float = line.ascent + line.descent + line.leading
                caretY += this.lineHeight * rawHeight
                continue
            }
            val charIdx: Int = lineCharIdx.coerceIn(0, line.characterCount)
            val caret = line.getCaretInfo(TextHitInfo.leading(charIdx))
            return Vector2f(caret[0], caretY)
        }
        return Vector2f(0f, 0f)
    }
    
    fun lineHeightPx(): Float {
        val lines: List<TextLayout> = this.renderState?.lines ?: return 0f
        val ll = lines.first()
        val rawHeight: Float = ll.ascent + ll.descent + ll.leading
        return rawHeight * this.lineHeight
    }
    
    override fun render(context: UiElementContext) {
        if (!this.renderIsDirty) { return }
        val (awtFont, lines, image, g) = this.renderState ?: return
        g.font = awtFont
        val c: Vector4fc = this.color ?: context.global.defaultFontColor
        g.color = Color(c.x(), c.y(), c.z(), c.w())
        var offsetY = 0f
        for (line in lines) {
            val x: Float = this.alignment
                .xPosOf(image.width.toFloat(), line.advance)
            val y: Float = offsetY + line.ascent
            line.draw(g, x, y)
            val rawHeight: Float = line.ascent + line.descent + line.leading
            offsetY += rawHeight * this.lineHeight
        }
        this.result?.dispose()
        this.result = Texture.fromBufferedImage(image, Texture.Filter.LINEAR)
        this.renderIsDirty = false
    }
    
    fun withText(text: String): Text {
        this.value = text
        return this
    }
    
    fun withFont(font: Font?): Text {
        this.font = font
        return this
    }
    
    fun withSize(size: UiSize?): Text {
        this.fontSize = size
        return this
    }
    
    fun withWrapping(enabled: Boolean = true): Text {
        this.wrapText = enabled
        return this
    }
    
    fun withAlignment(alignment: Alignment): Text {
        this.alignment = alignment
        return this
    }

    fun withLineHeight(lineHeight: Float): Text {
        this.lineHeight = lineHeight
        return this
    }
    
    fun alignLeft(): Text = this.withAlignment(Alignment.LEFT)
    fun alignCenter(): Text = this.withAlignment(Alignment.CENTER)
    fun alignRight(): Text = this.withAlignment(Alignment.RIGHT)
    
}

fun text(value: String, size: UiSize? = null): Text
    = Text().withText(value).withSize(size)