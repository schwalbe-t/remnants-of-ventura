
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.Disposable
import schwalbe.ventura.engine.gfx.Texture
import kotlin.math.roundToInt

data class UiElementContext(
    val global: UiScreen<*>, val parent: UiElement?,
    val absPxX: Int, val absPxY: Int,
    val parentPxWidth: Int, val parentPxHeight: Int,
    val visibleAbsLeft: Int, val visibleAbsTop: Int,
    val visibleAbsRight: Int, val visibleAbsBottom: Int
)

fun UiElementContext.childContext(
    parent: UiElement?, relPxX: Int, relPxY: Int, width: Int, height: Int
): UiElementContext {
    val pWidth: Int = maxOf(width, 0)
    val pHeight: Int = maxOf(height, 0)
    val absPxLeft: Int = this.absPxX + maxOf(relPxX, 0)
    val absPxTop: Int = this.absPxY + maxOf(relPxY, 0)
    val absPxRight: Int = absPxLeft + pWidth
    val absPxBottom: Int = absPxTop + pHeight
    return UiElementContext(
        this.global, parent,
        this.absPxX + relPxX, this.absPxY + relPxY, pWidth, pHeight,
        this.closestVisibleX(absPxLeft), this.closestVisibleY(absPxTop),
        this.closestVisibleX(absPxRight), this.closestVisibleY(absPxBottom)
    )
}

fun UiElementContext.closestVisibleX(absX: Int): Int
    = absX.coerceIn(this.visibleAbsLeft, this.visibleAbsRight)
fun UiElementContext.closestVisibleY(absY: Int): Int
    = absY.coerceIn(this.visibleAbsTop, this.visibleAbsBottom)

abstract class UiElement : Disposable {
    
    private var isDirty: Boolean = true
    var wasDisposed: Boolean = false
        private set
    
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
    
    protected open fun updateChildren(
        context: UiElementContext, f: (UiElement, UiElementContext) -> Unit
    ) {
        val childContext = context.childContext(
            this, 0, 0, this.pxWidth, this.pxHeight
        )
        for (child in this.children) {
            f(child, childContext)
        }
    }
    
    open fun captureInput(context: UiElementContext) {
        this.updateChildren(context, UiElement::captureInput)
    }
    
    protected open fun updateLayout(context: UiElementContext) {}
    
    fun update(context: UiElementContext) {
        if (this.isDirty) {
            this.pxWidth = maxOf(this.width(context).roundToInt(), 1)
            this.pxHeight = maxOf(this.height(context).roundToInt(), 1)
            this.updateLayout(context)
            this.children.forEach(UiElement::invalidate)
        }
        this.updateChildren(context, UiElement::update)
        if (this.isDirty) {
            context.parent?.invalidate()
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
        this.wasDisposed = true
    }
    
    fun disposeTree() {
        this.dispose()
        this.children.forEach(UiElement::disposeTree)
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
