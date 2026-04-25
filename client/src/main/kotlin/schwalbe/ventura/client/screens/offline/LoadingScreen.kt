
package schwalbe.ventura.client.screens.offline

import schwalbe.ventura.client.Client
import schwalbe.ventura.client.LocalKeys.TITLE_LOADING
import schwalbe.ventura.client.localized
import schwalbe.ventura.client.screens.GameScreen
import schwalbe.ventura.client.screens.googleSansSb
import schwalbe.ventura.client.screens.noNetworkConnections
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.ui.*

fun loadLoadingScreenResources(loader: ResourceLoader) = loader.submitAll(
    googleSansSb
)

fun loadingScreen(client: Client): () -> GameScreen = {
    val screen = GameScreen(
        networkState = noNetworkConnections(client),
        navigator = client.nav
    )
    screen.add(layer = 0, element = FlatBackground().withColor(15, 15, 15))
    screen.add(layer = 1, element = Text()
        .withText(localized()[TITLE_LOADING])
        .withFont(googleSansSb())
        .withSize(80.ph)
        .pad(top = 100.ph - 3.vmin)
        .pad(3.vmin)
    )
    screen
}
