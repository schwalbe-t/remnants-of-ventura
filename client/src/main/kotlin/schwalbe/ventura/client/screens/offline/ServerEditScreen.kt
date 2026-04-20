
package schwalbe.ventura.client.screens.offline

import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.screens.*

const val DEFAULT_SERVER_PORT: Int = 443

fun serverEditScreen(
    server: Config.Server?,
    client: Client,
    result: (Config.Server) -> Unit
): () -> GameScreen = {
    val background = WorldBackground(backgroundWorld(), client)
    val screen = GameScreen(
        render = background::render,
        networkState = noNetworkConnections(client),
        navigator = client.nav,
        onClose = background::dispose
    )
    val l = localized()
    val settings = Axis.column()
    val nameInput = Theme.addLabelledInput(
        settings, l[LABEL_SERVER_NAME], l[PLACEHOLDER_SERVER_NAME],
        server?.name ?: ""
    )
    val addrInput = Theme.addLabelledInput(
        settings, l[LABEL_SERVER_ADDRESS], l[PLACEHOLDER_SERVER_ADDRESS],
        server?.address ?: "", valFont = jetbrainsMonoSb()
    )
    val portInput = Theme.addLabelledInput(
        settings, l[LABEL_SERVER_PORT], l[PLACEHOLDER_SERVER_PORT],
        server?.port?.toString() ?: "", valFont = jetbrainsMonoSb()
    )
    settings.add(6.vmin,
        Theme.button(
            content = l[BUTTON_SERVER_CONFIRM],
            handler = handler@{
                val port: String = portInput.valueString.trim()
                val parsedPort: Int = if (port.isEmpty()) DEFAULT_SERVER_PORT
                else {
                    port.toIntOrNull() ?: return@handler
                }
                client.nav.pop()
                result(
                    Config.Server(
                        nameInput.valueString.trim(),
                        addrInput.valueString.trim(),
                        parsedPort
                    )
                )
            }
        ).pad(top = 2.vmin, left = 30.pw, right = 30.pw)
    )
    settings.add(5.vmin,
        Theme.button(
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
    screen
}