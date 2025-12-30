
package schwalbe.ventura.engine.input

import org.joml.Vector2f
import org.joml.Vector2fc

object Mouse {
    
    private val buttonsDown: MutableSet<MButton> = mutableSetOf()
    val pressedButtons: Set<MButton> = this.buttonsDown
    
    private val actualPosition = Vector2f(0f, 0f)
    val position: Vector2fc = this.actualPosition
    
    fun handleEvent(e: InputEvent) { when(e) {
        is MButtonDown -> this.buttonsDown.add(e.button)
        is MButtonUp -> this.buttonsDown.remove(e.button)
        is MouseMove -> this.actualPosition.set(e.newPosition)
        else -> {}
    } }

}

val MButton.isPressed: Boolean
    get() = Mouse.pressedButtons.contains(this)