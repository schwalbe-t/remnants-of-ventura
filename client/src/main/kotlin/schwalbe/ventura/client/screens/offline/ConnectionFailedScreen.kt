
package schwalbe.ventura.client.screens.offline

import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.screens.*

fun serverConnectionFailedScreen(
    reason: String, client: Client
): () -> GameScreen = {
    val screen = GameScreen(
        render = renderGridBackground(client),
        networkState = noNetworkConnections(client),
        navigator = client.nav
    )
    val contSize: UiSize = 20.vmin
    screen.add(layer = 0, element = Axis.column()
        .add(50.ph - (contSize / 2), Space())
        .add(contSize, Axis.column()
            .add(5.vmin, Text()
                .withText(localized()[TITLE_CONNECTION_TO_SERVER_FAILED])
                .withSize(70.ph)
                .withAlignment(Text.Alignment.CENTER)
            )
            .add(3.vmin, Text()
                .withText(reason)
                .withFont(jetbrainsMonoSb())
                .withColor(SECONDARY_FONT_COLOR)
                .withSize(70.ph)
                .withAlignment(Text.Alignment.CENTER)
            )
            .add(7.vmin, Space())
            .add(5.vmin,
                createTextButton(
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
