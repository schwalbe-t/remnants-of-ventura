
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Framebuffer
import schwalbe.ventura.engine.gfx.Texture
import org.joml.Vector4f
import org.joml.Vector4fc

class MsaaRenderDisplay(
    samples: Int, renderContents: () -> Unit = {}
) : GpuUiElement() {

    companion object {
        val DEFAULT_CLEAR_COLOR: Vector4fc = Vector4f(0f, 0f, 0f, 0f)
        const val DEFAULT_CLEAR_DEPTH: Float = 1f
    }


    override val children: List<UiElement> = listOf()

    val msaaTarget: Framebuffer = Framebuffer()
        .attachColor(Texture(
            16, 16, Texture.Filter.NEAREST, Texture.Format.RGBA8, samples
        ))
        .attachDepth(Texture(
            16, 16, Texture.Filter.NEAREST, Texture.Format.DEPTH24, samples
        ))

    var renderContents: () -> Unit = renderContents
        set(value) {
            field = value
            this.invalidate()
        }

    var clearColor: Vector4fc = MsaaRenderDisplay.DEFAULT_CLEAR_COLOR
        set(value) {
            field = Vector4f(value)
            this.invalidate()
        }
    var clearDepth: Float = MsaaRenderDisplay.DEFAULT_CLEAR_DEPTH
        set(value) {
            if (field != value) {
                field = value
                this.invalidate()
            }
        }

    override fun render(context: UiElementContext) {
        this.prepareTarget()
        this.msaaTarget.resize(this.target.width, this.target.height)
        this.msaaTarget.clearColor(this.clearColor)
        this.msaaTarget.clearDepth(this.clearDepth)
        this.renderContents()
        this.msaaTarget.blitColorOnto(this.target)
    }

    override fun dispose() {
        super.dispose()
        this.msaaTarget.dispose()
    }

    fun withRenderedContent(f: () -> Unit): MsaaRenderDisplay {
        this.renderContents = f
        return this
    }

    fun withClearColor(clearColor: Vector4fc): MsaaRenderDisplay {
        this.clearColor = clearColor
        return this
    }

    fun withClearDepth(clearDepth: Float): MsaaRenderDisplay {
        this.clearDepth = clearDepth
        return this
    }

}