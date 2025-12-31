
package schwalbe.ventura.engine.input

import org.lwjgl.glfw.GLFW.*
import org.joml.Vector2fc
import org.joml.Vector2f

sealed interface InputEvent

sealed interface MouseEvent : InputEvent
sealed class MButtonEvent(val button: MButton) : MouseEvent
class MButtonDown(button: MButton) : MButtonEvent(button)
class MButtonUp(button: MButton) : MButtonEvent(button)
class MouseMove(val newPosition: Vector2fc) : MouseEvent
class MouseScroll(val offset: Vector2fc) : MouseEvent

sealed interface KeyboardEvent : InputEvent
sealed class KeyEvent(val key: Key) : KeyboardEvent
class KeyDown(key: Key) : KeyEvent(key)
class KeyRepeat(key: Key) : KeyEvent(key)
class KeyUp(key: Key) : KeyEvent(key)
class CharTyped(val codepoint: Int) : KeyboardEvent

class InputEventQueue(windowId: Long) {
    
    private val queued: MutableList<InputEvent> = mutableListOf()
    private val queuedRemaining: MutableList<InputEvent> = mutableListOf()
    
    private fun add(e: InputEvent) {
        this.queued.add(e)
        this.queuedRemaining.add(e)
    }
    
    init {
        glfwSetMouseButtonCallback(windowId) { _, glfwButton, action, _ ->
            val button: MButton = glfwMButtonMap[glfwButton]
                ?: return@glfwSetMouseButtonCallback
            when (action) {
                GLFW_PRESS      -> this.add(MButtonDown(button))
                GLFW_RELEASE    -> this.add(MButtonUp(button))
            }
        }
        glfwSetCursorPosCallback(windowId) { _, posX, posY ->
            val pos = Vector2f(posX.toFloat(), posY.toFloat())
            this.add(MouseMove(pos))
        }
        glfwSetScrollCallback(windowId) { _, offsetX, offsetY ->
            val pos = Vector2f(offsetX.toFloat(), offsetY.toFloat())
            this.add(MouseScroll(pos))
        }
        glfwSetKeyCallback(windowId) { _, glfwKey, scanCode, action, _ ->
            val key: Key = glfwKeyMap[glfwKey]
                ?: return@glfwSetKeyCallback
            when (action) {
                GLFW_PRESS      -> this.add(KeyDown(key))
                GLFW_REPEAT     -> this.add(KeyRepeat(key))
                GLFW_RELEASE    -> this.add(KeyUp(key))
            }
        }
        glfwSetCharCallback(windowId) { _, codepoint ->
            this.add(CharTyped(codepoint))
        }
    }
    
    /**
     * Returns all events that have happened since the last frame, including
     * those that have already been removed.
     */
    fun all(): List<InputEvent>
        = this.queued.toList()
    
    /**
     * Returns all events of the given type that have happened since the last
     * frame, including those that have already been removed.
     */
    inline fun <reified E : InputEvent> allOfType(): List<E>
        = this.all().filterIsInstance<E>()
    
    /**
     * Returns all remaining events that have happened since the last frame.
     * All events returned by this function may be removed using the [remove]
     * method and will be added to the states of [Mouse] and [Keyboard] if left
     * unremoved.
     */
    fun remaining(): List<InputEvent>
        = this.queuedRemaining.toList()
    
    /**
     * Returns all remaining events of the given type that have happened since
     * the last frame. All events returned by this function may be removed
     * using the [remove] method and will be added to the states of [Mouse]
     * and [Keyboard] if left unremoved.
     */
    inline fun <reified E : InputEvent> remainingOfType(): List<E>
        = this.remaining().filterIsInstance<E>()
    
    /**
     * Removes the event from the queue, claiming it. This removes the event
     * from the lists returned by [remaining] and [remainingOfType], but not
     * from those returned by [all] and [allOfType]. The given event will not
     * be added to the states of the [Mouse] and [Keyboard] objects.
     */
    fun remove(event: InputEvent)
        = this.queuedRemaining.removeIf { it === event }
    
    /**
     * Removes all events from the queue, including those returned by
     * [all], [allOfType], [remaining] and [remainingOfType].
     */
    fun clear() {
        this.queued.clear()
        this.queuedRemaining.clear()
    }

}