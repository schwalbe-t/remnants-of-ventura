
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*

const val DEFAULT_SERVER_PORT: Int = 443

private fun addSetting(
    axis: Axis, name: String, placeholder: String, value: String,
    font: Font
): TextInput {
    val input = TextInput()
    axis.add(3.vmin, Text()
        .withText(name)
        .withSize(2.vmin)
        .pad(bottom = 0.5.vmin, left = 1.vmin)
    )
    axis.add(7.vmin, createTextInput(input, placeholder, value, font)
        .pad(bottom = 2.vmin)
    )
    return input
}

fun serverEditScreen(
    server: Config.Server?,
    client: Client,
    result: (Config.Server) -> Unit
): UiScreenDef = defineScreen(
    defaultFontColor = BASE_FONT_COLOR
) {
    client.onFrame = renderGridBackground(client)
    val l = localized()
    val settings = Axis.column()
    val nameInput = addSetting(
        settings, l[LABEL_SERVER_NAME], l[PLACEHOLDER_SERVER_NAME],
        server?.name ?: "", googleSansR()
    )
    val addrInput = addSetting(
        settings, l[LABEL_SERVER_ADDRESS], l[PLACEHOLDER_SERVER_ADDRESS],
        server?.address ?: "", jetbrainsMonoSb()
    )
    val portInput = addSetting(
        settings, l[LABEL_SERVER_PORT], l[PLACEHOLDER_SERVER_PORT],
        server?.port?.toString() ?: "", jetbrainsMonoSb()
    )
    settings.add(6.vmin,
        createTextButton(
            content = l[BUTTON_SERVER_CONFIRM],
            handler = handler@{
                val port: String = portInput.valueString
                val parsedPort: Int = if (port.isEmpty()) DEFAULT_SERVER_PORT
                    else { port.toIntOrNull() ?: return@handler }
                client.nav.pop()
                result(Config.Server(
                    nameInput.valueString, addrInput.valueString,
                    parsedPort
                ))
            }
        )
        .pad(top = 2.vmin, left = 30.pw, right = 30.pw)
    )
    settings.add(5.vmin,
        createTextButton(
            content = l[BUTTON_SERVER_DISCARD],
            handler = {
                client.nav.pop()
            }
        )
        .pad(top = 1.vmin, left = 30.pw, right = 30.pw)
    )
    it.add(layer = 0, element = Axis.column()
        .add(7.ph, Text()
            .withFont(googleSansSb())
            .withText(l[TITLE_EDIT_SERVER])
            .withSize(2.5.vmin)
        )
        .add((100 - 7).ph, settings)
        .pad(5.vmin)
    )
}