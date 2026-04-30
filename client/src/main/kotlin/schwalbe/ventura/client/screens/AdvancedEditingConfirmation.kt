
package schwalbe.ventura.client.screens

import schwalbe.ventura.client.Client
import schwalbe.ventura.client.LocalKeys
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.localized
import schwalbe.ventura.client.write
import schwalbe.ventura.engine.ui.*

private fun makeEditingModeSection(
    name: LocalKeys,
    description: LocalKeys,
    onSelect: () -> Unit
): UiElement = Axis.column()
    .add(4.vmin, Text()
        .withText(localized()[name])
        .withSize(80.ph)
        .withFont(googleSansSb())
    )
    .add(1.vmin, Space())
    .add(100.ph - 4.vmin - 1.vmin - 1.vmin - 4.vmin, Text()
        .withText(localized()[description])
        .withSize(1.75.vmin)
        .wrapThemedScrolling(horiz = false, vert = true)
    )
    .add(1.vmin, Space())
    .add(4.vmin, Stack()
        .add(FlatBackground()
            .withColor(Theme.BUTTON_COLOR)
            .withHoverColor(Theme.BUTTON_HOVER_COLOR)
        )
        .add(Text()
            .withText(localized()[BUTTON_SELECT_MODE].replace(
                "{MODE}", localized()[name]
            ))
            .withSize(80.ph)
            .alignCenter()
            .pad(1.vmin)
        )
        .add(ClickArea().withLeftHandler(onSelect))
        .wrapBorderRadius(0.75.vmin)
        .pad(horizontal = 10.pw, vertical = 0.px)
    )
    .pad(horizontal = 5.vmin, vertical = 20.ph)

fun showAdvancedEditingConfirmation(
    client: Client,
    onSelect: (isEnabled: Boolean) -> Unit = {},
    layer: Int = 100
) {
    val screen = client.nav.currentOrNull ?: return
    val renderBase = screen.render
    var background: BlurBackground? = BlurBackground()
        .withRadius(3)
        .withSpread(5)
    val container = Stack()
    screen.render = {
        renderBase()
        background?.invalidate()
    }
    fun close() {
        background = null
        container.disposeAll()
    }
    container.add(background ?: Space())
        .add(FlatBackground().withColor(Theme.PANEL_BACKGROUND))
        .add(Axis.row(50.pw)
            .add(makeEditingModeSection(
                NAME_SIMPLE_EDITING_MODE, DESCRIPTION_SIMPLE_EDITING_MODE
            ) {
                close()
                onSelect(false)
            })
            .add(makeEditingModeSection(
                NAME_ADVANCED_EDITING_MODE, DESCRIPTION_ADVANCED_EDITING_MODE
            ) {
                client.config.settings.advancedEditingEnabled = true
                client.config.write()
                close()
                onSelect(true)
            })
        )
    screen.add(layer = layer, element = container)
}
