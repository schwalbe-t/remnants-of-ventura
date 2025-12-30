
package schwalbe.ventura.engine.input

import org.lwjgl.glfw.GLFW.*
import org.joml.Vector2fc
import org.joml.Vector2f

sealed interface InputEvent
data class MButtonDown(val button: MButton) : InputEvent
data class MButtonUp(val button: MButton) : InputEvent
data class MouseMove(val newPosition: Vector2fc) : InputEvent
data class MouseScroll(val offset: Vector2fc) : InputEvent
data class KeyDown(val key: Key) : InputEvent
data class KeyUp(val key: Key) : InputEvent
data class CharTyped(val codepoint: Int) : InputEvent

class InputEventQueue(windowId: Long) {
    
    private val queued: MutableList<InputEvent> = mutableListOf()
    
    init {
        glfwSetMouseButtonCallback(windowId) { _, glfwButton, action, _ ->
            val button: MButton = glfwMButtonMap[glfwButton]
                ?: return@glfwSetMouseButtonCallback
            when (action) {
                GLFW_PRESS      -> this.queued.add(MButtonDown(button))
                GLFW_RELEASE    -> this.queued.add(MButtonUp(button))
            }
        }
        glfwSetCursorPosCallback(windowId) { _, posX, posY ->
            val pos = Vector2f(posX.toFloat(), posY.toFloat())
            this.queued.add(MouseMove(pos))
        }
        glfwSetScrollCallback(windowId) { _, offsetX, offsetY ->
            val pos = Vector2f(offsetX.toFloat(), offsetY.toFloat())
            this.queued.add(MouseScroll(pos))
        }
        glfwSetKeyCallback(windowId) { _, glfwKey, scanCode, action, _ ->
            val key: Key = glfwKeyMap[glfwKey]
                ?: return@glfwSetKeyCallback
            when (action) {
                GLFW_PRESS      -> this.queued.add(KeyDown(key))
                GLFW_RELEASE    -> this.queued.add(KeyUp(key))
            }
        }
        glfwSetCharCallback(windowId) { _, codepoint ->
            this.queued.add(CharTyped(codepoint))
        }
    }
    
    fun all(): List<InputEvent>
        = this.queued.toList()
    
    inline fun <reified E : InputEvent> allOfType(): List<E>
        = this.all().filterIsInstance<E>()
    
    fun remove(event: InputEvent)
        = this.queued.removeIf { it === event }
    
    fun clear()
        = this.queued.clear()

}