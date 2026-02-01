
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Texture
import schwalbe.ventura.engine.gfx.Shader
import org.joml.*
import kotlin.math.roundToInt

class Padding : GpuUiElement() {
    
    var inside: UiElement? = null
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
    
    override fun updateChildren(
        context: UiElementContext, f: (UiElement, UiElementContext) -> Unit
    ) {
        val inside: UiElement = this.inside ?: return
        val left: Int = this.paddingLeft(context).roundToInt()
        val right: Int = this.paddingRight(context).roundToInt()
        val top: Int = this.paddingTop(context).roundToInt()
        val bottom: Int = this.paddingBottom(context).roundToInt()
        val innerWidth: Int = this.pxWidth - (left + right)
        val innerHeight: Int = this.pxHeight - (top + bottom)
        if (innerWidth < 0 || innerHeight < 0) { return }
        val childContext = context.childContext(
            this, left, top, innerWidth, innerHeight
        )
        f(inside, childContext)
    }
   
    override fun render(context: UiElementContext) {
        val inside: UiElement = this.inside ?: return
        val insideTex: Texture = inside.result ?: return
        this.prepareTarget()
        val insideOffsetX: Int = this.paddingLeft(context).roundToInt()
        val insideOffsetY: Int = this.paddingTop(context).roundToInt()
        blitTexture(
            insideTex, this.target,
            insideOffsetX, insideOffsetY,
            inside.pxWidth, inside.pxHeight
        )
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
    
    fun withPadding(
        top: UiSize, bottom: UiSize, left: UiSize, right: UiSize
    ): Padding {
        this.paddingTop = top
        this.paddingBottom = bottom
        this.paddingLeft = left
        this.paddingRight = right
        this.invalidate()
        return this
    }
    
    fun withContent(inside: UiElement?): Padding {
        this.inside = inside
        this.invalidate()
        return this
    }
    
    fun withoutContent(): Padding {
        this.inside?.disposeTree()
        this.inside = null
        this.invalidate()
        return this
    }
    
}

fun UiElement.pad(amount: UiSize): Padding
    = Padding().withPadding(amount).withContent(this)
    
fun UiElement.pad(horizontal: UiSize, vertical: UiSize): Padding
    = Padding().withPadding(horizontal, vertical).withContent(this)
    
fun UiElement.pad(
    top: UiSize = 0.px, bottom: UiSize = 0.px,
    left: UiSize = 0.px, right: UiSize = 0.px
): Padding
    = Padding().withPadding(top, bottom, left, right).withContent(this)