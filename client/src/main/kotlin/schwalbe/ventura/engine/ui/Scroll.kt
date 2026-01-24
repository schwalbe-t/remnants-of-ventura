
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Texture
import schwalbe.ventura.engine.gfx.Shader
import schwalbe.ventura.engine.gfx.ConstFramebuffer
import schwalbe.ventura.engine.input.*
import org.joml.*
import kotlin.math.roundToInt

private data class Rectangle(
    val x: Int = 0, val y: Int = 0, val w: Int = 0, val h: Int = 0
)

private class ScrollBar(
    var track: Rectangle = Rectangle(),
    var thumb: Rectangle = Rectangle(),
    var barLength: Int = 0,
    var insideLength: Int = 0,
    var thumbHover: Boolean = false,
    var anchorPos: Pair<Int, Int>? = null
) {
    
    companion object {
        val trackColor: Vector4fc = Vector4f(0f, 0f, 0f, 0f)
        val thumbColor: Vector4fc = Vector4f(0.5f, 0.5f, 0.5f, 0.5f)
        val thumbHoverColor: Vector4fc = Vector4f(0.75f, 0.75f, 0.75f, 0.5f)
        
        val defaultWidth: UiSize = 1.vmin
    }
    
    
    fun updateLayout(
        left: Int, top: Int, barWidth: Int, barLength: Int,
        dirX: Int, dirY: Int, orthoX: Int, orthoY: Int,
        scrollDistance: Int, innerDistance: Int
    ) {
        this.track = Rectangle(
            left, top,
            dirX * barLength + orthoX * barWidth,
            dirY * barLength + orthoY * barWidth
        )
        val thumbStart: Int = scrollDistance * barLength / innerDistance
        val thumbLength: Int = barLength * barLength / innerDistance
        this.thumb = Rectangle(
            left + dirX * thumbStart,
            top + dirY * thumbStart,
            dirX * thumbLength + orthoX * barWidth,
            dirY * thumbLength + orthoY * barWidth
        )
        this.barLength = barLength
        this.insideLength = innerDistance
    }
    
    fun updateThumb(
        baseX: Int, baseY: Int, dirX: Int, dirY: Int,
        element: UiElement?, inputEvents: InputEventQueue
    ): Float {
        val thumbX: Int = baseX + this.thumb.x
        val thumbY: Int = baseY + this.thumb.y
        val newThumbHover: Boolean = Mouse.isInsideArea(
            thumbX, thumbY, thumbX + this.thumb.w, thumbY + this.thumb.h
        )
        if (this.thumbHover != newThumbHover) {
            this.thumbHover = newThumbHover
            element?.invalidate()
        }
        val relMouseX: Int = Mouse.position.x().roundToInt() - baseX
        val relMouseY: Int = Mouse.position.y().roundToInt() - baseY
        val anchor: Pair<Int, Int>? = this.anchorPos
        if (anchor == null && this.thumbHover) {
            for (e in inputEvents.remainingOfType<MButtonDown>()) {
                if (e.button != MButton.LEFT) { continue }
                this.anchorPos = Pair(relMouseX, relMouseY)
                inputEvents.remove(e)
            }
        } else if (anchor != null) {
            for (e in inputEvents.remainingOfType<MButtonUp>()) {
                if (e.button != MButton.LEFT) { continue }
                this.anchorPos = null
                inputEvents.remove(e)
                return 0f
            }
            val (anchorX, anchorY) = anchor
            this.anchorPos = Pair(relMouseX, relMouseY)
            val offsetX: Int = (relMouseX - anchorX) * dirX
            val offsetY: Int = (relMouseY - anchorY) * dirY
            val screenDist: Int = offsetX + offsetY
            return (screenDist * this.insideLength / this.barLength).toFloat()
        }
        return 0f
    }
    
    fun render(target: ConstFramebuffer) {
        fillColor(
            ScrollBar.trackColor, target,
            this.track.x, this.track.y, this.track.w, this.track.h
        )
        val thumbColor = if (!this.thumbHover) { ScrollBar.thumbColor }
            else { ScrollBar.thumbHoverColor }
        fillColor(
            thumbColor, target,
            this.thumb.x, this.thumb.y, this.thumb.w, this.thumb.h
        )
    }
    
}

class Scroll : GpuUiElement() {
    
    var inside: UiElement? = null
        private set
    
    override val children: List<UiElement>
        get() = listOfNotNull(this.inside)
    
    var scrollOffsetX: Float = 0f
        set(value) {
            if (field != value) { this.invalidate() }
            field = value
        }
    var scrollOffsetY: Float = 0f
        set(value) {
            if (field != value) { this.invalidate() }
            field = value
        }
    
    var scrollBarWidth: UiSize = ScrollBar.defaultWidth
        set(value) {
            field = value
            this.invalidate()
        }
    var showHorizBar: Boolean = true
        set(value) {
            if (field != value) { this.invalidate() }
            field = value
        }
    var showVertBar: Boolean = true
        set(value) {
            if (field != value) { this.invalidate() }
            field = value
        }
        
    private var shouldShowHorizBar: Boolean = false
    private var shouldShowVertBar: Boolean = false
    
    private var barWidth: Int = 0
    private var hBarHeight: Int = 0
    private var vBarWidth: Int = 0
    
    private var horizBar: ScrollBar? = null
    private var vertBar: ScrollBar? = null
    
    private fun updateDimensions(context: UiElementContext) {
        val inside: UiElement = this.inside ?: return
        val bw: Int = this.scrollBarWidth(context).roundToInt()
        this.barWidth = bw
        // bar display and widths
        this.shouldShowVertBar = this.showVertBar
            && inside.pxHeight > this.pxHeight
        this.vBarWidth = if (this.shouldShowVertBar) { bw } else { 0 }
        this.shouldShowHorizBar = this.showHorizBar
            && inside.pxWidth > this.pxWidth
        this.hBarHeight = if (this.shouldShowHorizBar) { bw } else { 0 }
        // bar positions and dimensions
        val showHoriz: Boolean = this.shouldShowHorizBar
        if (showHoriz != (this.horizBar != null)) {
            this.horizBar = if (showHoriz) { ScrollBar() } else { null }
        }
        val horizLength: Int = this.pxWidth - this.vBarWidth
        this.horizBar?.updateLayout(
            0, this.pxHeight - this.barWidth, bw, horizLength,
            dirX = 1, dirY = 0, orthoX = 0, orthoY = 1,
            this.scrollOffsetX.roundToInt(), inside.pxWidth
        )
        val showVert: Boolean = this.shouldShowVertBar
        if (showVert != (this.vertBar != null)) {
            this.vertBar = if (showVert) { ScrollBar() } else { null }
        }
        val vertLength: Int = this.pxHeight
        this.vertBar?.updateLayout(
            this.pxWidth - this.barWidth, 0, bw, vertLength,
            dirX = 0, dirY = 1, orthoX = 1, orthoY = 0,
            this.scrollOffsetY.roundToInt(), inside.pxHeight
        )
    }
    
    override fun updateLayout(context: UiElementContext)
        = this.updateDimensions(context)
        
    private fun limitScrollOffsets() {
        val inside: UiElement = this.inside ?: return
        val horizScrollLimit: Int
            = maxOf(inside.pxWidth - this.pxWidth + this.vBarWidth, 0)
        val vertScrollLimit: Int
            = maxOf(inside.pxHeight - this.pxHeight + this.hBarHeight, 0)
        this.scrollOffsetX = this.scrollOffsetX
            .coerceIn(0f, horizScrollLimit.toFloat())
        this.scrollOffsetY = this.scrollOffsetY
            .coerceIn(0f, vertScrollLimit.toFloat())
    }
        
    override fun captureInput(context: UiElementContext) {
        this.scrollOffsetX += this.horizBar?.updateThumb(
            context.absPxX, context.absPxY, 1, 0, this, context.global.nav.input
        ) ?: 0f
        this.scrollOffsetY += this.vertBar?.updateThumb(
            context.absPxX, context.absPxY, 0, 1, this, context.global.nav.input
        ) ?: 0f
        this.updateChildren(context, UiElement::captureInput)
        val inside: UiElement = this.inside ?: return
        val rawAbsRight: Int = context.absPxX + this.pxWidth - this.vBarWidth
        val rawAbsBottom: Int = context.absPxY + this.pxHeight - this.hBarHeight
        val isInside: Boolean = Mouse.isInsideArea(
            context.visibleAbsLeft, context.visibleAbsTop,
            context.closestVisibleX(rawAbsRight),
            context.closestVisibleY(rawAbsBottom)
        )
        if (isInside) {
            val lineHeight: Float = context.global.nav.defaultFontSize(context)
            for (e in context.global.nav.input.remainingOfType<MouseScroll>()) {
                if (this.shouldShowHorizBar) {
                    this.scrollOffsetX -= e.offset.x() * lineHeight
                }
                if (this.shouldShowVertBar) {
                    this.scrollOffsetY -= e.offset.y() * lineHeight
                }
                context.global.nav.input.remove(e)
                this.invalidate()
            }
        }
        this.limitScrollOffsets()
    }
    
    override fun updateChildren(
        context: UiElementContext, f: (UiElement, UiElementContext) -> Unit
    ) {
        val inside: UiElement = this.inside ?: return
        val childContext = context.childContext(
            this,
            -this.scrollOffsetX.roundToInt(),
            -this.scrollOffsetY.roundToInt(),
            this.pxWidth - this.vBarWidth,
            this.pxHeight - this.hBarHeight
        )
        f(inside, childContext)
    }
    
    override fun render(context: UiElementContext) {
        val inside: UiElement = this.inside ?: return
        val insideTex: Texture = inside.result ?: return
        this.updateDimensions(context)
        this.prepareTarget()
        blitTexture(
            insideTex, this.target,
            -this.scrollOffsetX.roundToInt(),
            -this.scrollOffsetY.roundToInt(),
            inside.pxWidth, inside.pxHeight
        )
        this.horizBar?.render(this.target)
        this.vertBar?.render(this.target)
    }
    
    fun requestVisible(relPosX: Int, relPosY: Int, padding: Int = 0) {
        val outerX: Int = relPosX - this.scrollOffsetX.roundToInt()
        val outerY: Int = relPosY - this.scrollOffsetY.roundToInt()
        val deltaLeft = 0 - (outerX - padding)
        val deltaTop = 0 - (outerY - padding)
        val deltaRight = (outerX + padding) - (this.pxWidth - this.vBarWidth)
        val deltaBottom = (outerY + padding) - (this.pxHeight - this.hBarHeight)
        if (deltaLeft > 0) { this.scrollOffsetX -= deltaLeft }
        if (deltaRight > 0) { this.scrollOffsetX += deltaRight }
        if (deltaTop > 0) { this.scrollOffsetY -= deltaTop }
        if (deltaBottom > 0) { this.scrollOffsetY += deltaBottom }
        this.limitScrollOffsets()
    }
    
    fun withBarsEnabled(horiz: Boolean = true, vert: Boolean = true): Scroll {
        this.showHorizBar = horiz
        this.showVertBar = vert
        return this
    }
    
    fun withBarWidth(width: UiSize = ScrollBar.defaultWidth): Scroll {
        this.scrollBarWidth = width
        return this
    }
    
    fun withContent(inside: UiElement?): Scroll {
        this.inside = inside
        this.invalidate()
        return this
    }
    
    fun withoutContent(): Scroll {
        this.inside = null
        this.invalidate()
        return this
    }
    
}

fun UiElement.wrapScrolling(
    horiz: Boolean = true, vert: Boolean = true,
    barWidth: UiSize = ScrollBar.defaultWidth
): Scroll
    = Scroll()
        .withBarsEnabled(horiz, vert)
        .withBarWidth(barWidth)
        .withContent(this)
