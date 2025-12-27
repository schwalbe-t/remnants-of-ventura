
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Framebuffer
import schwalbe.ventura.engine.gfx.Texture
import kotlin.math.roundToInt

abstract class GpuUiElement : UiElement() {
    
    protected val target = Framebuffer()
    
    protected fun prepareTarget() {
        val resultWidth: Int = this.pxWidth.roundToInt()
        val resultHeight: Int = this.pxHeight.roundToInt()
        val oldResult: Texture? = this.result
        val makeNewTex: Boolean = oldResult == null
            || oldResult.width != resultWidth
            || oldResult.height != resultHeight
        if (makeNewTex) {
            val oldTexture: Texture? = this.result
            this.result = Texture(
                resultWidth, resultHeight,
                Texture.Filter.LINEAR, Texture.Format.RGBA8
            )
            this.target.attachColor(this.result)
            oldTexture?.dispose()
        }
    }
    
    override fun dispose() {
        super.dispose()
        this.target.dispose()
    }
    
}