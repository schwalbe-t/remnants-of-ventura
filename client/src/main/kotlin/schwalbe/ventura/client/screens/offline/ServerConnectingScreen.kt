
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
    val root = Stack()
    val background = WorldBackground(backgroundWorld(), client)
    val screen = GameScreen(
        render = {
            background.render()
            root.invalidate()
        },
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
        navigator = client.nav,
        onClose = background::dispose
    )
    val contSize: UiSize = 22.vmin
    root.add(Axis.column()
        .add(50.ph - (contSize / 2), Space())
        .add(contSize, Axis.column()
            .add(5.vmin + 2.vmin, Stack()
                .add(BlurBackground().withRadius(5))
                .add(FlatBackground().withColor(Theme.PANEL_BACKGROUND))
                .add(Text()
                    .withText(name)
                    .withFont(jetbrainsMonoSb())
                    .withColor(Theme.SECONDARY_FONT_COLOR)
                    .withSize(2.vmin)
                    .alignCenter()
                    .pad(1.vmin)
                )
                .wrapBorderRadius(0.75.vmin)
                .pad(left = 20.pw, right = 20.pw)
            )
            .add(3.vmin, Space())
            .add(5.vmin,
                Theme.button(
                    content = localized()[BUTTON_CANCEL_CONNECTION],
                    handler = {
                        client.nav.pop()
                    }
                ).pad(left = 30.pw, right = 30.pw)
            )
        )
        .add(50.ph - (contSize / 2), Space())
        .pad(5.vmin)
        .withTitlebar(localized()[TITLE_CONNECTING_TO_SERVER])
    )
    screen.add(layer = 0, element = root)
    screen
}
