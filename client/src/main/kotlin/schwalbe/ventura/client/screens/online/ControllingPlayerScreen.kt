
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.engine.input.*

fun controllingPlayerScreen(client: Client): GameScreen {
    val screen = GameScreen(
        render = {
            if (Key.ESCAPE.wasPressed) {
                client.nav.push(escapeMenuScreen(client))
            }
        },
        networkState = keepNetworkConnectionAlive(client, onFail = { reason ->
            client.nav.replace(serverConnectionFailedScreen(reason, client))
            client.network.clearError()
        }),
        packets = createPacketHandler(),
        navigator = client.nav
    )
    return screen
}

