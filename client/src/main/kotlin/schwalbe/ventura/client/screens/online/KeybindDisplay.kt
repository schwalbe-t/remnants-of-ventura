
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.LocalKeys
import schwalbe.ventura.client.localized
import schwalbe.ventura.client.screens.Theme
import schwalbe.ventura.client.screens.googleSansSb
import schwalbe.ventura.client.screens.jetbrainsMonoB
import schwalbe.ventura.engine.ui.*

class Keybind(
    val name: LocalKeys,
    val keys: List<Key>,
    val displayIf: () -> Boolean = { true }
) {
    class Key(val name: String, val width: Float = 1f)

    var lastDisplayed: Boolean = false
}

class KeybindDisplay(
    val keybinds: List<Keybind>,
    val width: UiSize = 20.vw
) {

    val blurBackground = BlurBackground().withRadius(2).withSpread(4)

    val displayed = Axis.column()
    val dispPad: UiSize = 0.5.vmin

    var root = Stack()
        .add(this.blurBackground)
        .add(FlatBackground().withColor(Theme.PANEL_BACKGROUND))
        .add(this.displayed.pad(this.dispPad))
        .wrapBorderRadius(0.75.vmin)

    fun fitContent() {
        this.root.withHeight(
            this.displayed.computeContentLength() + 2 * this.dispPad
        )
    }

    private fun render() {
        this.displayed.disposeAll()
        for (keybind in this.keybinds) {
            if (!keybind.lastDisplayed) { continue }
            val buttons = Axis.row()
            for (key in keybind.keys) {
                buttons.add(100.ph * key.width, Stack()
                    .add(FlatBackground().withColor(Theme.FONT_COLOR))
                    .add(Text()
                        .withFont(jetbrainsMonoB())
                        .withText(key.name)
                        .withSize(75.ph)
                        .withColor(10, 10, 10)
                        .alignCenter()
                        .pad(0.1.vmin)
                    )
                    .wrapBorderRadius(0.5.vmin)
                    .pad(0.5.vmin)
                )
            }
            val buttonsLen: UiSize = buttons.computeContentLength()
            this.displayed.add(3.vmin, Axis.row()
                .add(100.pw - buttonsLen, Text()
                    .withText(localized()[keybind.name])
                    .withFont(googleSansSb())
                    .withSize(80.ph)
                    .pad(0.75.vmin)
                )
                .add(buttonsLen, buttons)
            )
        }
    }

    fun update(isEnabled: Boolean) {
        this.blurBackground.invalidate()
        var isDirty = false
        for (keybind in this.keybinds) {
            val shouldDisplay: Boolean = keybind.displayIf()
            if (shouldDisplay == keybind.lastDisplayed) { continue }
            keybind.lastDisplayed = shouldDisplay
            isDirty = true
        }
        if (isDirty) { this.render() }
        if (isEnabled) {
            this.root.withWidth(this.width)
            this.fitContent()
        } else {
            this.root.withSize(0.px, 0.px)
        }
    }

    fun createRootMount(): UiElement = this.root
        .pad(left = 100.pw - this.width - 2.5.vmin, top = 2.5.vmin)

}
