
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Framebuffer
import schwalbe.ventura.engine.gfx.Texture
import org.joml.*

abstract class GpuUiElement : UiElement() {
    
    companion object {
        val clearColor: Vector4fc = Vector4f(0f, 0f, 0f, 0f)
    }
    
    protected val target = Framebuffer()
    
    protected fun prepareTarget() {
        val oldResult: Texture? = this.result
        val makeNewTex: Boolean = oldResult == null
            || oldResult.width != this.pxWidth
            || oldResult.height != this.pxHeight
        if (makeNewTex) {
            this.target.attachColor(null)
            this.result?.dispose()
            this.result = Texture(
                this.pxWidth, this.pxHeight,
                Texture.Filter.NEAREST, Texture.Format.RGBA8
            )
            this.target.attachColor(this.result)
        }
        this.target.clearColor(GpuUiElement.clearColor)
    }
    
    override fun dispose() {
        super.dispose()
        this.target.dispose()
    }
    
}