package schwalbe.ventura.client.screens.offline

import schwalbe.ventura.client.Client
import schwalbe.ventura.client.screens.*

fun offlineSettingsScreen(client: Client): () -> GameScreen = {
    val background = WorldBackground(backgroundWorld(), client)
    settingsScreen(
        client,
        networkState = noNetworkConnections(client),
        renderBackground = { root ->
            background.render()
            root.invalidate()
        },
        onClose = background::dispose
    )()
}
