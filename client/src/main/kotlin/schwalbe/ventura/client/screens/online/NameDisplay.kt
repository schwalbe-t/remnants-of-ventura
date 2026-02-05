
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.screens.BRIGHT_FONT_COLOR
import schwalbe.ventura.client.screens.PANEL_BACKGROUND
import schwalbe.ventura.client.screens.googleSansSb
import schwalbe.ventura.engine.ui.*

object NameDisplay {

    fun createDisplay(username: String): UiElement = Stack()
        .add(BlurBackground()
            .withRadius(2)
            .withSpread(2)
        )
        .add(FlatBackground()
            .withColor(PANEL_BACKGROUND)
        )
        .add(Text()
            .withText(username)
            .withColor(BRIGHT_FONT_COLOR)
            .withFont(googleSansSb())
            .withSize(1.2.vmin)
            .withWrapping(enabled = false)
            .alignCenter()
            .pad(1.vmin)
        )
        .wrapBorderRadius(0.75.vmin)
        .withHeight(3.5.vmin)

    fun updateDisplay(display: UiElement) {
        val container: UiElement = display.children.getOrNull(0)
            ?: return
        val textPad: Padding = container.children.getOrNull(2)
            as? Padding ?: return
        val text: Text = textPad.children.getOrNull(0)
            as? Text ?: return
        val paddingHoriz: UiSize = textPad.paddingLeft + textPad.paddingRight
        display.width = maxOf(
            text.pxTextWidth.px + floor(paddingHoriz),
            15.vmin
        )
    }

}