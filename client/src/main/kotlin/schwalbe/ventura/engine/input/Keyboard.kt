
package schwalbe.ventura.engine.input

object Keyboard {
    
    private val keysDown: MutableSet<Key> = mutableSetOf()
    val pressedKeys: Set<Key> = this.keysDown
    
    fun handleEvent(e: InputEvent) { when (e) {
        is KeyDown -> keysDown.add(e.key)
        is KeyUp -> keysDown.remove(e.key)
        else -> {}
    } }
    
}

val Key.isPressed: Boolean
    get() = Keyboard.pressedKeys.contains(this)