
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Texture
import schwalbe.ventura.engine.gfx.Shader
import org.joml.*
import kotlin.math.roundToInt

class Padding : GpuUiElement(), UiContainer {
    
    override var inside: UiElement? = null
        private set
        
    var paddingTop: UiSize = 0.px
        private set
    var paddingBottom: UiSize = 0.px
        private set
    var paddingLeft: UiSize = 0.px
        private set
    var paddingRight: UiSize = 0.px
        private set
    
    override val children: List<UiElement>
        get() = listOfNotNull(this.inside)
    
    override fun updateLayout(context: UiElementContext) {
        val inside: UiElement = this.inside ?: return
        val paddingHoriz: UiSize = this.paddingLeft + this.paddingRight
        val paddingVert: UiSize = this.paddingTop + this.paddingBottom
        this.parentContext = UiParentContext(
            pxWidth = (this.pxWidth - paddingHoriz(context)).roundToInt(),
            pxHeight = (this.pxHeight - paddingVert(context)).roundToInt()
        )
    }
   
    override fun render(context: UiElementContext) {
        val inside: UiElement = this.inside ?: return
        val insideTex: Texture = inside.result ?: return
        this.prepareTarget()
        val insideOffsetX: Float = this.paddingLeft(context)
        val insideOffsetY: Float = this.paddingTop(context)
        val shader: Shader<PxPos, Blit> = blitShader()
        shader[PxPos.bufferSizePx] = Vector2f(
            this.target.width.toFloat(), this.target.height.toFloat()
        )
        shader[PxPos.destTopLeftPx] = Vector2f(insideOffsetX, insideOffsetY)
        shader[PxPos.destSizePx] = Vector2f(
            inside.pxWidth.toFloat(), inside.pxHeight.toFloat()
        )
        shader[Blit.texture] = insideTex
        quad().render(shader, this.target)
    }
    
    fun withPadding(amount: UiSize): Padding {
        this.paddingTop = amount
        this.paddingBottom = amount
        this.paddingLeft = amount
        this.paddingRight = amount
        this.invalidate()
        return this
    }
    
    fun withPadding(horizontal: UiSize, vertical: UiSize): Padding {
        this.paddingTop = vertical
        this.paddingBottom = vertical
        this.paddingLeft = horizontal
        this.paddingRight = horizontal
        this.invalidate()
        return this
    }
    
    fun withPaddingTop(amount: UiSize): Padding {
        this.paddingTop = amount
        this.invalidate()
        return this
    }
    
    fun withPaddingBottom(amount: UiSize): Padding {
        this.paddingBottom = amount
        this.invalidate()
        return this
    }
    
    fun withPaddingLeft(amount: UiSize): Padding {
        this.paddingLeft = amount
        this.invalidate()
        return this
    }
    
    fun withPaddingRight(amount: UiSize): Padding {
        this.paddingRight = amount
        this.invalidate()
        return this
    }
    
    fun withContents(inside: UiElement?): Padding {
        this.inside = inside
        this.invalidate()
        return this
    }
    
    fun withoutContents(): Padding {
        this.inside = null
        this.invalidate()
        return this
    }
    
    override fun setContents(inside: UiElement?) {
        this.inside = inside
        this.invalidate()
    }
    
}