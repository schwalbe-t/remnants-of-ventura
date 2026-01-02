
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Texture
import schwalbe.ventura.engine.gfx.fromBufferedImage
import org.joml.*
import java.awt.*
import java.awt.Font as AwtFont
import java.awt.font.*
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.text.AttributedCharacterIterator
import java.text.AttributedString
import kotlin.math.ceil
import kotlin.math.roundToInt

data class Span(
    val text: String, 
    val color: Vector4fc? = null,
    val font: Font? = null
)

fun Span.derive(newText: String): Span
    = Span(newText, this.color, this.font)

fun Iterable<Span>.splitLines(): List<List<Span>> {
    val allLines: MutableList<MutableList<Span>> = mutableListOf()
    var currentLine: MutableList<Span> = mutableListOf()
    allLines.add(currentLine)
    for (span in this) {
        var i = 0
        while (true) {
            val nl = span.text.indexOf('\n', i)
            if (nl == -1) {
                currentLine.add(span.derive(span.text.substring(i)))
                break
            }
            currentLine.add(span.derive(span.text.substring(i, nl)))
            currentLine = mutableListOf()
            allLines.add(currentLine)
            i = nl + 1
        }
    }
    return allLines
}

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

        private fun wrapText(
            paragraph: List<Span>,
            widthLimit: Float, fontSize: Float, lineHeightPx: Float,
            defaultFont: Font,
            defaultColor: Vector4fc
        ): SplitLines {
            val text = AttributedString(paragraph
                .joinToString("", transform = Span::text)
                .ifEmpty { " " }
            )
            var spanOffset = 0
            for (span in paragraph) {
                if (span.text.isEmpty()) { continue }  
                val start: Int = spanOffset
                spanOffset += span.text.length
                val end: Int = spanOffset
                val font: Font = span.font ?: defaultFont
                val color: Vector4fc = span.color ?: defaultColor
                text.addAttribute(
                    TextAttribute.FONT,
                    font.baseFont.deriveFont(fontSize),
                    start, end
                )
                text.addAttribute(
                    TextAttribute.FOREGROUND,
                    color,
                    start, end
                )
            }
            val chars: AttributedCharacterIterator = text.iterator
            val lineBreaks = LineBreakMeasurer(chars, Text.baseFrc)
            val lines = mutableListOf<Line>()
            var maxWidth = 0f
            var maxHeight = 0f
            while (lineBreaks.position < chars.endIndex) {
                val start: Int = lineBreaks.position
                val layout: TextLayout = lineBreaks.nextLayout(widthLimit)
                val end: Int = lineBreaks.position
                lines.add(Line(text, start, end, layout))
                val width: Float = maxOf(
                    layout.bounds.width.toFloat(),
                    minOf(layout.advance, widthLimit)
                )
                maxWidth = maxOf(maxWidth, width)
                maxHeight += lineHeightPx
            }
            return SplitLines(
                lines, ceil(maxWidth).toInt(), ceil(maxHeight).toInt()
            )
        }

    }
    
    private data class SplitLines(
        val lines: List<Line>,
        val maxWidth: Int,
        val maxHeight: Int
    )
    
    private data class Line(
        val text: AttributedString,
        val start: Int, val end: Int,
        val layout: TextLayout
    )
    
    private data class RenderState(
        val lines: List<Line>,
        val image: BufferedImage,
        val g: Graphics2D
    )
    
    
    private var renderIsDirty: Boolean = true
    
    var content: List<Span> = listOf()
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
    var lineHeightPx: Float = 0f
        private set
    
    override fun updateLayout(context: UiElementContext) {
        val fontSize: UiSize = this.fontSize
            ?: context.global.defaultFontSize
        val fontSizePx: Float = fontSize(context)
        val defaultFont: Font = this.font
            ?: context.global.defaultFont
        val defaultColor: Vector4fc = this.color
            ?: context.global.defaultFontColor
        val fm: FontMetrics = Text.baseGraphics.getFontMetrics(
            defaultFont.baseFont.deriveFont(fontSizePx)
        )
        val rawFontHeight: Int = fm.ascent + fm.descent + fm.leading
        this.lineHeightPx = rawFontHeight.toFloat() * this.lineHeight
        val widthLimit: Float = if (!this.wrapText) { Float.POSITIVE_INFINITY }
            else { this.pxWidth.toFloat() }
        val paragraphs: List<SplitLines> = this.content.splitLines()
            .map { p -> Text.wrapText(
                p, widthLimit, fontSizePx, this.lineHeightPx,
                defaultFont, defaultColor
            ) }
            .toList()
        val lines: List<Line> = paragraphs.flatMap(SplitLines::lines)
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
        this.renderState = RenderState(lines, image, g)
    }
    
    fun charIdxOfPos(relPosX: Float, lineIdx: Int): Int? {
        val line: TextLayout = this.renderState?.lines
            ?.getOrNull(lineIdx)?.layout
            ?: return null
        val x: Float = relPosX.coerceIn(0f, line.advance)
        val hit = line.hitTestChar(x, 0f)
        return hit.insertionIndex
    }
    
    fun posOfCharIdx(lineCharIdx: Int, lineIdx: Int): Vector2f? {
        val lines: List<Line> = this.renderState?.lines ?: return null
        var caretY = 0f
        for ((lineI, line) in lines.withIndex()) {
            val ll: TextLayout = line.layout
            if (lineI != lineIdx) {
                caretY += this.lineHeightPx
                continue
            }
            val charIdx: Int = lineCharIdx.coerceIn(0, ll.characterCount)
            val caret = ll.getCaretInfo(TextHitInfo.leading(charIdx))
            return Vector2f(caret[0], caretY)
        }
        return Vector2f(0f, 0f)
    }
    
    override fun render(context: UiElementContext) {
        if (!this.renderIsDirty) { return }
        val (lines, image, g) = this.renderState ?: return
        var y = 0f
        for (line in lines) {
            val ll: TextLayout = line.layout
            var chars: AttributedCharacterIterator = line.text.iterator
            chars.index = line.start
            while (chars.index < line.end) {
                val runLimit: Int = minOf(
                    chars.getRunLimit(TextAttribute.FOREGROUND),
                    line.end
                )
                val color = chars
                    .getAttribute(TextAttribute.FOREGROUND) as? Vector4fc
                if (color != null) {
                    g.color = Color(color.x(), color.y(), color.z(), color.w())
                }
                val x: Float = this.alignment
                    .xPosOf(image.width.toFloat(), ll.advance)
                val startX: Float = x + ll.getCaretInfo(
                    TextHitInfo.leading(chars.index - line.start)
                )[0]
                val endX: Float = x + ll.getCaretInfo(
                    TextHitInfo.leading(runLimit - line.start)
                )[0]
                val oldClip: Shape? = g.clip
                g.clip = Rectangle2D.Float(
                    startX, y,
                    endX - startX, lineHeightPx
                )
                ll.draw(g, x, y + ll.ascent)
                g.clip = oldClip
                chars.index = runLimit
            }
            y += this.lineHeightPx
        }
        this.result?.dispose()
        this.result = Texture.fromBufferedImage(image, Texture.Filter.LINEAR)
        this.renderIsDirty = false
    }
    
    fun withText(text: String): Text {
        this.content = listOf(Span(text))
        return this
    }
    
    fun withText(content: List<Span>): Text {
        this.content = content
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