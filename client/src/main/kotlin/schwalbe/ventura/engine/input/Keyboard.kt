
package schwalbe.ventura.engine.input

object Keyboard {
    
    private val keysDown: MutableSet<Key> = mutableSetOf()
    private val keysDownStarted: MutableSet<Key> = mutableSetOf()
    val pressedKeys: Set<Key> = this.keysDown
    val startedPressingKeys: Set<Key> = this.keysDownStarted
    
    fun handleEvent(e: InputEvent) { when (e) {
        is KeyDown -> {
            this.keysDown.add(e.key)
            this.keysDownStarted.add(e.key)
        }
        is KeyUp -> this.keysDown.remove(e.key)
        else -> {}
    } }

    fun onFrameEnd() {
        this.keysDownStarted.clear()
    }
    
}

val Key.isPressed: Boolean
    get() = Keyboard.pressedKeys.contains(this)

val Key.wasPressed: Boolean
    get() = Keyboard.startedPressingKeys.contains(this)