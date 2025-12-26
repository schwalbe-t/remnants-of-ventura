
package schwalbe.ventura.engine.ui

class Padding : UiElement() {
    
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
    
    override fun updateLayout(context: UiElementContext) {
        val inside: UiElement = this.inside ?: return
        val paddingHoriz: UiSize = this.paddingLeft + this.paddingRight
        val paddingVert: UiSize = this.paddingTop + this.paddingBottom
        this.parentContext = UiParentContext(
            pxWidth = this.pxWidth - paddingHoriz(context),
            pxHeight = this.pxHeight - paddingVert(context)
        )
    }
   
    override fun render(context: UiElementContext) {
        val inside: UiElement = this.inside ?: return
        // TODO! prepare 'this.result' texture and framebuffer
        //       (possibly in superclass for all GPU rendered elements?)
        val insideOffsetX: Float = this.paddingLeft(context)
        val insideOffsetY: Float = this.paddingTop(context)
        // TODO! blit 'inside.result' onto 'this.result' at 'insideOffset(X/Y)'
    }
    
    fun withPadding(amount: UiSize): Padding {
        this.paddingTop = amount
        this.paddingBottom = amount
        this.paddingLeft = amount
        this.paddingRight = amount
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
    
    /** NOTE: Returns 'inside'! */
    fun <E : UiElement> withContents(inside: E): E {
        this.inside = inside
        this.invalidate()
        return inside
    }
    
    /** NOTE: Returns 'this'! */
    fun withoutContents(): UiElement {
        this.inside = null
        return this
    }
    
    fun setContents(inside: UiElement?) {
        this.inside = inside
    }
    
}