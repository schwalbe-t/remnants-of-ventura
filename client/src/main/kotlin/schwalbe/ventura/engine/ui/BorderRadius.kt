
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Texture
import schwalbe.ventura.engine.gfx.Shader
import org.joml.Vector2f
import kotlin.math.round

class BorderRadius : GpuUiElement() {
    
    var inside: UiElement? = null
        set(value) {
            field = value
            this.invalidate()
        }
        
    var radius: UiSize = 0.px
        set(value) {
            field = value
            this.invalidate()
        }
        
    override val children: List<UiElement>
        get() = listOfNotNull(this.inside)
    
    override fun render(context: UiElementContext) {
        val inside: UiElement = this.inside ?: return
        val insideTex: Texture = inside.result ?: return
        this.prepareTarget()
        val shader: Shader<FullBuffer, RoundedBlit> = roundedBlitShader()
        shader[RoundedBlit.texture] = insideTex
        shader[RoundedBlit.destSizePx] = Vector2f(
            inside.pxWidth.toFloat(), inside.pxHeight.toFloat()
        )
        shader[RoundedBlit.borderRadius] = round(this.radius(context))
        quad().render(shader, this.target)
    }
    
    fun withRadius(amount: UiSize): BorderRadius {
        this.radius = amount
        return this
    }
    
    fun withContent(inside: UiElement?): BorderRadius {
        this.inside = inside
        return this
    }
    
    fun withoutContent(): BorderRadius {
        this.inside = null
        return this
    }
        
}

fun UiElement.wrapBorderRadius(amount: UiSize): BorderRadius
    = BorderRadius().withRadius(amount).withContent(this)