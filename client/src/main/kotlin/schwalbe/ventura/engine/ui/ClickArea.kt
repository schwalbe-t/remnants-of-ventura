
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.input.MButton
import schwalbe.ventura.engine.input.MButtonUp
import schwalbe.ventura.engine.input.Mouse
import schwalbe.ventura.engine.input.isInsideArea

class ClickArea : UiElement() {
    
    var onClick: () -> Unit = {}
    
    override val children: List<UiElement> = listOf()
    
    override fun captureInput(context: UiElementContext){
        val pressed: Boolean = context.global.input
            .remainingOfType<MButtonUp>()
            .any { e -> e.button == MButton.LEFT && Mouse.isInsideArea(
                context.visibleAbsLeft, context.visibleAbsTop,
                context.visibleAbsRight, context.visibleAbsBottom
            ) }
        if (pressed) {
            this.onClick()
        }
    }
    
    fun withHandler(f: () -> Unit): ClickArea {
        this.onClick = f
        return this
    }
    
}