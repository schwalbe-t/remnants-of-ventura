
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Shader
import org.joml.*

class FlatBackground : GpuUiElement(), Colored {
    
    companion object {
        val defaultColor: Vector4fc = Vector4f(0f, 0f, 0f, 1f)
    }
    
    
    override var color: Vector4fc? = null
        set(value) {
            field = if (value == null) { null }
                else { Vector4f(value) }
            this.invalidate()
        }
    
    override val children: List<UiElement> = listOf()
    
    override fun render(context: UiElementContext) {
        this.prepareTarget()
        val shader: Shader<FullBuffer, FlatBg> = flatBgShader()
        shader[FlatBg.color] = this.color ?: FlatBackground.defaultColor
        quad().render(shader, this.target)
    }
    
}