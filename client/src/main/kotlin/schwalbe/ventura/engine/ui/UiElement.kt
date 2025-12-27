
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.Disposable
import schwalbe.ventura.engine.gfx.Texture
import kotlin.math.roundToInt

data class UiParentContext(
    val pxWidth: Int,
    val pxHeight: Int
)

data class UiElementContext(
    val global: UiContext,
    val parent: UiParentContext
)

abstract class UiElement : Disposable {
    
    private var isDirty: Boolean = true
    
    var width: UiSize = fpw
        set(value) {
            field = value
            this.invalidate()
        }
    var height: UiSize = fph
        set(value) {
            field = value
            this.invalidate()
        }
    
    var pxWidth: Int = 0
        protected set
    var pxHeight: Int = 0
        protected set
        
    var result: Texture? = null
        protected set
    
    abstract val children: List<UiElement>
    
    protected open fun updateLayout(context: UiElementContext) {}
    
    protected var parentContext: UiParentContext = UiParentContext(0, 0)
    
    fun update(context: UiElementContext) {
        if (this.isDirty) {
            this.pxWidth = this.width(context).roundToInt()
            this.pxHeight = this.height(context).roundToInt()
            this.parentContext = UiParentContext(this.pxWidth, this.pxHeight)
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
    
    protected open fun render(context: UiElementContext) {}
    
    fun invalidate() {
        this.isDirty = true
    }
    
    override fun dispose() {
        this.result?.dispose()
    }
    
}

fun <E: UiElement> E.withWidth(width: UiSize): E {
    this.width = width
    return this
}

fun <E: UiElement> E.withHeight(height: UiSize): E {
    this.height = height
    return this
}

fun <E: UiElement> E.withSize(width: UiSize, height: UiSize): E {
    this.width = width
    this.height = height
    return this
}

fun <E : UiElement> E.invalidated(): E {
    this.invalidate()
    return this
}
