
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Framebuffer
import schwalbe.ventura.engine.gfx.Texture
import org.joml.*

abstract class GpuUiElement : UiElement() {
    
    companion object {
        val clearColor: Vector4fc = Vector4f(0f, 0f, 0f, 0f)
    }
    
    protected val target = Framebuffer()
        .attachColor(Texture(
            16, 16,
            Texture.Filter.NEAREST, Texture.Format.RGBA8
        ))
    
    protected fun prepareTarget() {
        this.target.resize(maxOf(this.pxWidth, 16), maxOf(this.pxHeight, 16))
        this.result = this.target.color
        this.target.clearColor(GpuUiElement.clearColor)
    }
    
    override fun dispose() {
        super.dispose()
        this.target.dispose()
    }
    
}