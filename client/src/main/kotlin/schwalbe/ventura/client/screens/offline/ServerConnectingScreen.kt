
package schwalbe.ventura.client.screens.offline

import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.online.serverAuthenticationScreen

fun serverConnectingScreen(
    address: String, port: Int, client: Client
): () -> GameScreen = {
    client.network.connect(address, port)
    val name = "$address:$port"
    val screen = GameScreen(
        render = renderGridBackground(client),
        networkState = establishNetworkConnection(
            client,
            onSuccess = {
                client.nav.replace(serverAuthenticationScreen(name, client))
            },
            onFail = { reason ->
                client.nav.replace(serverConnectionFailedScreen(reason, client))
                client.network.clearError()
            }
        ),
        navigator = client.nav
    )
    val contSize: UiSize = 20.vmin
    screen.add(layer = 0, element = Axis.column()
        .add(50.ph - (contSize / 2), Space())
        .add(contSize, Axis.column()
            .add(5.vmin, Text()
                .withText(localized()[TITLE_CONNECTING_TO_SERVER])
                .withSize(70.ph)
                .alignCenter()
            )
            .add(3.vmin, Text()
                .withText(name)
                .withFont(jetbrainsMonoSb())
                .withColor(SECONDARY_FONT_COLOR)
                .withSize(70.ph)
                .alignCenter()
            )
            .add(7.vmin, Space())
            .add(5.vmin,
                createTextButton(
                    content = localized()[BUTTON_CANCEL_CONNECTION],
                    handler = {
                        client.nav.pop()
                    }
                ).pad(left = 30.pw, right = 30.pw)
            )
        )
        .add(50.ph - (contSize / 2), Space())
        .pad(5.vmin)
    )
    screen
}
