
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Texture

data class UiParentContext(
    val pxWidth: Float,
    val pxHeight: Float
)

data class UiElementContext(
    val global: UiContext,
    val parent: UiParentContext
)

abstract class UiElement {
    
    private var isDirty: Boolean = true
    
    var width: UiSize = fpw
        protected set
    var height: UiSize = fph
        protected set
    
    var pxWidth: Float = 0f
        protected set
    var pxHeight: Float = 0f
        protected set
        
    var result: Texture? = null
        private set
    
    protected abstract val children: List<UiElement>
    
    abstract fun updateLayout(context: UiElementContext)
    
    protected var parentContext: UiParentContext = UiParentContext(0f, 0f)
    
    fun update(context: UiElementContext) {
        if (this.isDirty) {
            this.pxWidth = this.width(context)
            this.pxHeight = this.height(context)
            this.parentContext = UiParentContext(
                this.pxWidth, this.pxHeight
            )
            this.updateLayout(context)
            this.children.forEach(UiElement::invalidate)
        }
        val childContext = UiElementContext(context.global, this.parentContext)
        for (child in this.children) {
            child.update(childContext)
        }
        if (this.isDirty) {
            this.render(context)
        }
        this.isDirty = false
    }
    
    abstract fun render(context: UiElementContext)
    
    fun setWidth(width: UiSize) {
        this.width = width
        this.invalidate()
    }
    
    fun setHeight(height: UiSize) {
        this.height = height
        this.invalidate()
    }
    
    fun invalidate() {
        this.isDirty = true
    }
    
}

fun <E: UiElement> E.withWidth(width: UiSize): E {
    this.setWidth(width)
    return this
}

fun <E: UiElement> E.withHeight(height: UiSize): E {
    this.setHeight(height)
    return this
}

fun <E : UiElement> E.invalidated(): E {
    this.invalidate()
    return this
}
