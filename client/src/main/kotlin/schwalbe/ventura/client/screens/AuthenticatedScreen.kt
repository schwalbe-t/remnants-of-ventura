
package schwalbe.ventura.client.screens

import schwalbe.ventura.client.*

fun serverAuthenticatedScreen(client: Client): GameScreen {
    val screen = GameScreen(
        render = {},
        networkState = keepNetworkConnectionAlive(client, onFail = { reason ->
            client.nav.replace(serverConnectionFailedScreen(reason, client))
            client.network.clearError()
        }),
        packets = createPacketHandler(),
        navigator = client.nav
    )
    return screen
}

