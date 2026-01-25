
package schwalbe.ventura.client.screens.offline

import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.screens.*

fun mainScreen(client: Client): () -> GameScreen = {
    val screen = GameScreen(
        render = renderGridBackground(client),
        networkState = noNetworkConnections(client),
        navigator = client.nav
    )
    val contSize: UiSize = 17.vmin
    screen.add(layer = 0, element = Axis.column()
        .add(50.ph - (contSize / 2), Space())
        .add(contSize, Axis.column()
            .add(5.vmin, createTextButton(
                content = localized()[BUTTON_PLAY],
                handler = {
                    client.nav.push(serverSelectScreen(client))
                }
            ))
            .add(1.vmin, Space())
            .add(5.vmin, createTextButton(
                content = localized()[BUTTON_CHANGE_LANGUAGE],
                handler = {
                    client.nav.push(languageSelectScreen(client))
                }
            ))
            .add(1.vmin, Space())
            .add(5.vmin, createTextButton(
                content = localized()[BUTTON_EXIT],
                handler = {
                    client.window.close()
                }
            ))
            .pad(left = 30.pw, right = 30.pw)
        )
        .add(50.ph - (contSize / 2), Space())
    )
    screen
}
