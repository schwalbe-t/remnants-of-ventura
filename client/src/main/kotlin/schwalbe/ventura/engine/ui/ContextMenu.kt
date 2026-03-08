
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Texture
import schwalbe.ventura.engine.input.MButtonUp
import schwalbe.ventura.engine.input.Mouse
import schwalbe.ventura.engine.input.isInsideArea
import kotlin.math.roundToInt

class ContextMenu : GpuUiElement() {

    private val container = Padding()
    private var x: Float = 0f
    private var y: Float = 0f
    private var canClose: Boolean = true

    override val children: List<UiElement> = listOf(this.container)

    override fun render(context: UiElementContext) {
        this.prepareTarget()
        val content: Texture = this.container.result ?: return
        blitTexture(content, this.target, 0, 0, content.width, content.height)
    }

    override fun captureInput(context: UiElementContext) {
        super.captureInput(context)
        val released: Boolean
            = context.global.nav.input.all().any { it is MButtonUp }
        if (!released) {
            this.canClose = true
        }
        val left: Int = context.visibleAbsLeft + this.x.roundToInt()
        val top: Int = context.visibleAbsTop + this.y.roundToInt()
        val menu: UiElement = this.container.children.getOrNull(0) ?: return
        val mouseOver: Boolean = Mouse.isInsideArea(
            left, top,
            minOf(left + menu.pxWidth, context.visibleAbsRight),
            minOf(top + menu.pxHeight, context.visibleAbsBottom)
        )
        if (released && this.canClose && !mouseOver) {
            this.close()
        }
    }

    fun open(menu: UiElement) {
        this.close()
        this.container.withContent(menu)
        this.x = Mouse.position.x()
        this.y = Mouse.position.y()
        this.container.withPadding(
            left = this.x.px, top = this.y.px,
            bottom = 0.px, right = 0.px
        )
        this.canClose = false
    }

    fun close() {
        this.container.disposeContent()
    }

}
