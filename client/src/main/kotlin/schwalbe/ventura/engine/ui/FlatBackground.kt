
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Shader
import schwalbe.ventura.engine.input.Mouse
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
        
    var hoverColor: Vector4fc? = null
        set(value) {
            field = if(value == null) { null }
                else { Vector4f(value) }
            this.invalidate()
        }
    
    private var hovering: Boolean = false
        
    override val children: List<UiElement> = listOf()
    
    override fun captureInput(context: UiElementContext) {
        val nowHovering: Boolean = context.absPxX <= Mouse.position.x()
            && context.absPxY <= Mouse.position.y()
            && Mouse.position.x() < context.absPxX + this.pxWidth
            && Mouse.position.y() < context.absPxY + this.pxHeight
        if (nowHovering == this.hovering) { return }
        this.hovering = nowHovering
        context.global.invalidate()
    }
    
    override fun render(context: UiElementContext) {
        this.prepareTarget()
        val hoverColor: Vector4fc?
            = if (this.hovering) { this.hoverColor } else { null }
        val shader: Shader<FullBuffer, FlatBg> = flatBgShader()
        shader[FlatBg.color] = hoverColor
            ?: this.color
            ?: FlatBackground.defaultColor
        quad().render(shader, this.target)
    }
    
    fun withHoverColor(color: Vector4fc?): FlatBackground {
        this.hoverColor = color
        return this
    }
    
    fun withHoverColor(color: Vector3fc, alpha: Float = 1f): FlatBackground {
        this.hoverColor = Vector4f(color, alpha)
        return this
    }
    
    fun withHoverColor(r: Int, g: Int, b: Int, a: Int = 255): FlatBackground {
        this.hoverColor = Vector4f(r / 255f, g / 255f, b / 255f, a / 255f)
        return this
    }
    
}