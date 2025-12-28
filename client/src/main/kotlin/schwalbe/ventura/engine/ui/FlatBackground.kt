
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Shader
import org.joml.*

class FlatBackground : GpuUiElement(), Colored {
    
    private val actualColor: Vector4f = Vector4f(1f, 1f, 1f, 1f)
    override var color: Vector4fc
        get() = this.actualColor
        set(value) {
            this.actualColor.set(value)
            this.invalidate()
        }
    
    override val children: List<UiElement> = listOf()
    
    override fun render(context: UiElementContext) {
        this.prepareTarget()
        val shader: Shader<FullBuffer, FlatBg> = flatBgShader()
        shader[FlatBg.color] = this.actualColor
        quad().render(shader, this.target)
    }
    
}