
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Shader
import org.joml.*

class FlatBackground : GpuUiElement() {
    
    var color: Vector4f = Vector4f(1f, 1f, 1f, 1f)
        private set
    
    override val children: List<UiElement> = listOf()
    
    override fun render(context: UiElementContext) {
        this.prepareTarget()
        val shader: Shader<FullBuffer, FlatBg> = flatBgShader()
        shader[FlatBg.color] = this.color
        quad().render(shader, this.target)
    }
    
    fun withColor(color: Vector4fc): FlatBackground {
        this.color.set(color)
        this.invalidate()
        return this
    }
    
    fun withColor(color: Vector3fc, alpha: Float = 1f): FlatBackground {
        this.color.set(color, alpha)
        this.invalidate()
        return this
    }
    
    fun withRgbColor(r: Int, g: Int, b: Int, a: Int = 255): FlatBackground {
        this.color.set(r / 255f, g / 255f, b / 255f, a / 255f)
        this.invalidate()
        return this
    }
    
}