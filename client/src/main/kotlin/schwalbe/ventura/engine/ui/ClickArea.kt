
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.input.MButton
import schwalbe.ventura.engine.input.MButtonUp
import schwalbe.ventura.engine.input.Mouse
import schwalbe.ventura.engine.input.isInsideArea

class ClickArea : UiElement() {
    
    var onClick: () -> Unit = {}

    override val children: List<UiElement> = listOf()
    
    override fun captureInput(context: UiElementContext) {
        for (e in context.global.input.remainingOfType<MButtonUp>()) {
            if (e.button != MButton.LEFT) { continue }
            if (!Mouse.isInsideArea(
                context.visibleAbsLeft, context.visibleAbsTop,
                context.visibleAbsRight, context.visibleAbsBottom
            )) { continue }
            context.global.input.remove(e)
            this.onClick()
        }
    }
    
    fun withHandler(f: () -> Unit): ClickArea {
        this.onClick = f
        return this
    }
    
}