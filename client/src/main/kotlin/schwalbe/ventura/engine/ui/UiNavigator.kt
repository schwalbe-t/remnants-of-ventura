
package schwalbe.ventura.engine.ui

import org.joml.Vector4f
import org.joml.Vector4fc
import schwalbe.ventura.engine.gfx.Framebuffer
import schwalbe.ventura.engine.input.InputEventQueue

class UiNavigator<S : UiScreen<S>>(
    val output: Framebuffer,
    val input: InputEventQueue,
    var defaultFont: Font = Font.default,
    var defaultFontSize: UiSize = 16.px,
    var defaultFontColor: Vector4fc = Vector4f(0f, 0f, 0f, 1f)
) {

    private val screens: MutableList<S> = mutableListOf()
    private var disposeQueue: MutableList<S> = mutableListOf()

    val current: S
        get() = this.screens.last()
    val currentOrNull: S?
        get() = this.screens.lastOrNull()

    fun push(screen: S) {
        this.screens.add(screen)
    }

    fun pop() {
        if (this.screens.size <= 1) { return }
        val removed: S = this.screens.removeLast()
        this.disposeQueue.add(removed)
    }

    fun replace(screen: S) {
        if (this.screens.isNotEmpty()) { this.pop() }
        this.push(screen)
    }

    fun clear(screen: S) {
        this.screens.forEach(this.disposeQueue::add)
        this.screens.clear()
        this.push(screen)
    }

    fun captureInput() {
        if (this.screens.isNotEmpty()) {
            this.screens.last().captureInput()
        }
    }

    fun update() {
        if (this.screens.isNotEmpty()) {
            this.screens.last().update()
        }
        this.disposeQueue.forEach(UiScreen<S>::disposeTree)
        this.disposeQueue.clear()
    }

}