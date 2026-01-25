
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

    private val screens: MutableList<() -> S> = mutableListOf()
    private var disposeQueue: MutableList<S> = mutableListOf()

    var currentOrNull: S? = null
        private set

    private fun disposeCurrent() {
        val current: S = this.currentOrNull ?: return
        this.disposeQueue.add(current)
        this.currentOrNull = null
    }

    fun push(screen: () -> S) {
        this.disposeCurrent()
        this.screens.add(screen)
        this.currentOrNull = screen()
    }

    fun pop() {
        if (this.screens.size < 2) { return }
        this.disposeCurrent()
        this.screens.removeLast()
        this.currentOrNull = this.screens.last().invoke()
    }

    fun replace(screen: () -> S) {
        if (this.screens.isNotEmpty()) { this.pop() }
        this.push(screen)
    }

    fun clear(screen: () -> S) {
        this.disposeCurrent()
        this.screens.clear()
        this.push(screen)
    }

    fun captureInput() {
        this.currentOrNull?.captureInput()
    }

    fun update() {
        this.currentOrNull?.update()
        this.disposeQueue.forEach(UiScreen<S>::disposeTree)
        this.disposeQueue.clear()
    }

}