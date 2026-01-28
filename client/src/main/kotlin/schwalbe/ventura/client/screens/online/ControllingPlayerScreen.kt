
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.net.PacketHandler

fun controllingPlayerScreen(client: Client): () -> GameScreen = {
    val renderWorld = renderGameworld(client)
    val screen = GameScreen(
        render = {
            if (Key.ESCAPE.wasPressed) {
                client.nav.push(escapeMenuScreen(client))
            }
            renderWorld()
        },
        networkState = keepNetworkConnectionAlive(client, onFail = { reason ->
            client.nav.replace(serverConnectionFailedScreen(reason, client))
            client.network.clearError()
        }),
        packets = PacketHandler<Unit>()
            .addErrorLogging()
            .addWorldHandling(client),
        navigator = client.nav
    )
    screen
}

