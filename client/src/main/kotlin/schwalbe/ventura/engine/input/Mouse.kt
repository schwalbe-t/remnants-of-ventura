
package schwalbe.ventura.engine.input

import org.lwjgl.glfw.GLFW.*
import org.joml.Vector2f
import org.joml.Vector2fc

enum class Cursor(val glfwCursorConst: Int) {
    ARROW       (GLFW_ARROW_CURSOR),
    IBEAM       (GLFW_IBEAM_CURSOR),
    CROSSHAIR   (GLFW_CROSSHAIR_CURSOR),
    HAND        (GLFW_HAND_CURSOR),
    HRESIZE     (GLFW_HRESIZE_CURSOR),
    VRESIZE     (GLFW_VRESIZE_CURSOR);
}

object Mouse {
    
    private val buttonsDown: MutableSet<MButton> = mutableSetOf()
    val pressedButtons: Set<MButton> = this.buttonsDown
    
    private val actualPosition = Vector2f(0f, 0f)
    val position: Vector2fc = this.actualPosition
    
    var cursor: Cursor = Cursor.ARROW
    
    fun handleEvent(e: InputEvent) { when(e) {
        is MButtonDown -> this.buttonsDown.add(e.button)
        is MButtonUp -> this.buttonsDown.remove(e.button)
        is MouseMove -> this.actualPosition.set(e.newPosition)
        else -> {}
    } }

}

fun Mouse.isInsideArea(left: Int, top: Int, right: Int, bottom: Int): Boolean
    = left <= this.position.x()
    && top <= this.position.y()
    && this.position.x() < right
    && this.position.y() < bottom
    
fun Mouse.isInsideArea(min: Vector2fc, max: Vector2fc): Boolean
    = min.x() <= this.position.x()
    && min.y() <= this.position.y()
    && this.position.x() < max.x()
    && this.position.y() < max.y()

val MButton.isPressed: Boolean
    get() = Mouse.pressedButtons.contains(this)