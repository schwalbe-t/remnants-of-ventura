
package schwalbe.ventura.engine.ui

import org.joml.Vector4f
import org.joml.Vector4fc
import schwalbe.ventura.engine.gfx.Framebuffer
import schwalbe.ventura.engine.input.InputEventQueue

class UiNavigator(
    val output: Framebuffer,
    val input: InputEventQueue,
    var defaultFont: Font = Font.default,
    var defaultFontSize: UiSize = 16.px,
    var defaultFontColor: Vector4fc = Vector4f(0f, 0f, 0f, 1f)
) {

    private val screens: MutableList<UiScreen> = mutableListOf()

    val current: UiScreen
        get() = screens.last()

    fun push(screen: UiScreenDef) {
        val s = UiScreen(
            screen, output, input,
            screen.defaultFont ?: this.defaultFont,
            screen.defaultFontSize ?: this.defaultFontSize,
            screen.defaultFontColor ?: this.defaultFontColor
        )
        screen.builder(s)
        this.screens.add(s)
    }

    fun pop() {
        if (this.screens.size <= 1) { return }
        val removed: UiScreen = this.screens.removeLast()
        removed.disposeTree()
    }

    fun clear(screen: UiScreenDef) {
        this.screens.forEach(UiScreen::disposeTree)
        this.screens.clear()
        this.push(screen)
    }

    fun captureInput() {
        screens.reversed().forEach(UiScreen::captureInput)
    }

    fun update() {
        screens.forEach(UiScreen::update)
    }

}