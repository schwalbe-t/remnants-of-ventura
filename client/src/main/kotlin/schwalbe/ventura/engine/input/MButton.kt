
package schwalbe.ventura.engine.input

import org.lwjgl.glfw.GLFW.*

enum class MButton {
    LEFT, MIDDLE, RIGHT
}

val glfwMButtonMap: Map<Int, MButton> = mapOf(
    GLFW_MOUSE_BUTTON_LEFT      to MButton.LEFT,
    GLFW_MOUSE_BUTTON_MIDDLE    to MButton.MIDDLE,
    GLFW_MOUSE_BUTTON_RIGHT     to MButton.RIGHT
)