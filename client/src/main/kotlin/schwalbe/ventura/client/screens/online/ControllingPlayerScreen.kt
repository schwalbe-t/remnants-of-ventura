
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.net.PacketHandler

fun controllingPlayerScreen(client: Client): () -> GameScreen = {
    client.world?.camController?.mode = CameraController.PLAYER_AT_CENTER
    val screen = GameScreen(
        render = {
            if (Key.ESCAPE.wasPressed) {
                client.nav.push(escapeMenuScreen(client))
            }
            if (Key.TAB.wasPressed) {
                client.nav.push(inventoryMenuScreen(client))
            }
            client.world?.update(client, captureInput = true)
            client.world?.render(client)
        },
        networkState = keepNetworkConnectionAlive(client, onFail = { reason ->
            client.nav.replace(serverConnectionFailedScreen(reason, client))
            client.network.clearError()
        }),
        packets = PacketHandler.receiveDownPackets<Unit>()
            .addErrorLogging()
            .addWorldHandling(client),
        navigator = client.nav
    )
    screen
}

