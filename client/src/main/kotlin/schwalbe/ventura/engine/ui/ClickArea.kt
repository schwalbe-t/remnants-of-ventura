
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.input.*

class ClickArea : UiElement() {
    
    var onLeftClick: (() -> Unit)? = null
    var onRightClick: (() -> Unit)? = null

    override val children: List<UiElement> = listOf()
    
    override fun captureInput(context: UiElementContext) {
        for (e in context.global.nav.input.remainingOfType<MButtonEvent>()) {
            if (!Mouse.isInsideArea(
                context.visibleAbsLeft, context.visibleAbsTop,
                context.visibleAbsRight, context.visibleAbsBottom
            )) { continue }
            context.global.nav.input.remove(e)
            if (e !is MButtonUp) { continue }
            val lmb = this.onLeftClick
            val rmb = this.onRightClick
            when (e.button) {
                MButton.LEFT    if lmb != null -> lmb()
                MButton.RIGHT   if rmb != null -> rmb()
                else -> {}
            }
        }
    }

    fun withLeftHandler(f: (() -> Unit)?): ClickArea {
        this.onLeftClick = f
        return this
    }

    fun withRightHandler(f: (() -> Unit)?): ClickArea {
        this.onRightClick = f
        return this
    }
    
}