
package schwalbe.ventura.client.screens

import schwalbe.ventura.client.Config
import schwalbe.ventura.client.localized
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.engine.ui.*

private fun addSetting(
    axis: Axis, name: String, placeholder: String, value: String
): TextInput {
    val input = TextInput()
    axis.add(3.vmin, Text()
        .withText(name)
        .withSize(2.vmin)
        .pad(bottom = 0.5.vmin, left = 1.vmin)
    )
    axis.add(7.vmin, createTextInput(input, placeholder, value)
        .pad(bottom = 2.vmin)
    )
    return input
}

fun serverEditScreen(
    server: Config.Server?,
    config: Config, nav: UiNavigator,
    result: (Config.Server) -> Unit
): UiScreenDef
= defineScreen(
    defaultFontColor = BASE_FONT_COLOR
) {
    val l = localized()
    val settings = Axis.column()
    val nameInput = addSetting(
        settings, l[LABEL_SERVER_NAME], l[PLACEHOLDER_SERVER_NAME],
        server?.name ?: ""
    )
    val addrInput = addSetting(
        settings, l[LABEL_SERVER_ADDRESS], l[PLACEHOLDER_SERVER_ADDRESS],
        server?.address ?: ""
    )
    val portInput = addSetting(
        settings, l[LABEL_SERVER_PORT], l[PLACEHOLDER_SERVER_PORT],
        server?.port?.toString() ?: ""
    )
    settings.add(6.vmin,
        createTextButton(
            content = l[BUTTON_SERVER_CONFIRM],
            handler = handler@{
                val parsedPort: Int = portInput.valueString.toIntOrNull()
                    ?: return@handler
                result(Config.Server(
                    nameInput.valueString, addrInput.valueString,
                    parsedPort
                ))
                nav.clear(serverSelectScreen(config, nav))
            }
        )
        .pad(top = 2.vmin, left = 30.pw, right = 30.pw)
    )
    settings.add(5.vmin,
        createTextButton(
            content = l[BUTTON_SERVER_DISCARD],
            handler = { nav.clear(serverSelectScreen(config, nav)) }
        )
        .pad(top = 1.vmin, left = 30.pw, right = 30.pw)
    )
    it.add(layer = -1, element = FlatBackground()
        .withColor(BACKGROUND_COLOR)
    )
    it.add(layer = 0, element = Axis.column()
        .add(7.ph, Text()
            .withText(l[TITLE_EDIT_SERVER])
            .withSize(2.5.vmin)
        )
        .add((100 - 7).ph, settings)
        .pad(5.vmin)
    )
}