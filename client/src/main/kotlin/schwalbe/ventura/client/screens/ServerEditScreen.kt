
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*

const val DEFAULT_SERVER_PORT: Int = 443

fun serverEditScreen(
    server: Config.Server?,
    client: Client,
    result: (Config.Server) -> Unit
): GameScreen {
    val screen = GameScreen(
        render = renderGridBackground(client),
        networkState = noNetworkConnections(client),
        navigator = client.nav
    )
    val l = localized()
    val settings = Axis.column()
    val nameInput = addLabelledInput(
        settings, l[LABEL_SERVER_NAME], l[PLACEHOLDER_SERVER_NAME],
        server?.name ?: "", googleSansR()
    )
    val addrInput = addLabelledInput(
        settings, l[LABEL_SERVER_ADDRESS], l[PLACEHOLDER_SERVER_ADDRESS],
        server?.address ?: "", jetbrainsMonoSb()
    )
    val portInput = addLabelledInput(
        settings, l[LABEL_SERVER_PORT], l[PLACEHOLDER_SERVER_PORT],
        server?.port?.toString() ?: "", jetbrainsMonoSb()
    )
    settings.add(6.vmin,
        createTextButton(
            content = l[BUTTON_SERVER_CONFIRM],
            handler = handler@{
                val port: String = portInput.valueString.trim()
                val parsedPort: Int = if (port.isEmpty()) DEFAULT_SERVER_PORT
                    else { port.toIntOrNull() ?: return@handler }
                client.nav.pop()
                result(Config.Server(
                    nameInput.valueString.trim(), addrInput.valueString.trim(),
                    parsedPort
                ))
            }
        ).pad(top = 2.vmin, left = 30.pw, right = 30.pw)
    )
    settings.add(5.vmin,
        createTextButton(
            content = l[BUTTON_SERVER_DISCARD],
            handler = {
                client.nav.pop()
            }
        ).pad(top = 1.vmin, left = 30.pw, right = 30.pw)
    )
    screen.add(layer = 0, element = Axis.column()
        .add(7.ph, Text()
            .withFont(googleSansSb())
            .withText(l[TITLE_EDIT_SERVER])
            .withSize(2.5.vmin)
        )
        .add((100 - 7).ph, settings)
        .pad(5.vmin)
    )
    return screen
}