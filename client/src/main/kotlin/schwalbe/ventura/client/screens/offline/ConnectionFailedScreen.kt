
package schwalbe.ventura.client.screens.offline

import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.screens.*

fun serverConnectionFailedScreen(
    reason: String, client: Client
): () -> GameScreen = {
    val background = WorldBackground(backgroundWorld(), client)
    val screen = GameScreen(
        render = background::render,
        networkState = noNetworkConnections(client),
        navigator = client.nav,
        onClose = background::dispose
    )
    val contSize: UiSize = 22.vmin
    screen.add(layer = 0, element = Axis.column()
        .add(50.ph - (contSize / 2), Space())
        .add(contSize, Axis.column()
            .add(5.vmin + 5.vmin + 2.vmin, Stack()
                .add(BlurBackground().withRadius(5))
                .add(FlatBackground().withColor(Theme.PANEL_BACKGROUND))
                .add(Axis.column()
                    .add(5.vmin, Text()
                        .withText(localized()[TITLE_CONNECTION_TO_SERVER_FAILED])
                        .withSize(70.ph)
                        .alignCenter()
                    )
                    .add(5.vmin, Text()
                        .withText(reason)
                        .withFont(jetbrainsMonoSb())
                        .withColor(Theme.SECONDARY_FONT_COLOR)
                        .withSize(2.vmin)
                        .alignCenter()
                    )
                    .pad(1.vmin)
                )
                .wrapBorderRadius(0.75.vmin)
                .pad(left = 20.pw, right = 20.pw)
            )
            .add(3.vmin, Space())
            .add(5.vmin,
                Theme.button(
                    content = localized()[BUTTON_GO_BACK],
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
