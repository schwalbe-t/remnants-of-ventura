
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Texture
import schwalbe.ventura.engine.input.MButtonUp
import schwalbe.ventura.engine.input.Mouse

class ContextMenu : GpuUiElement() {

    private val container = Padding()
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
        if (released && this.canClose) {
            this.close()
        }
    }

    fun open(menu: UiElement) {
        this.close()
        this.container.withContent(menu)
        this.container.withPadding(
            left = Mouse.position.x().px, top = Mouse.position.y().px,
            bottom = 0.px, right = 0.px
        )
        this.canClose = false
    }

    fun close() {
        this.container.disposeContent()
    }

}
