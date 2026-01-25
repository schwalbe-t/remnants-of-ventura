
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.net.PacketType.*

fun controllingPlayerScreen(client: Client): () -> GameScreen = {
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
        packets = createPacketHandler()
            .onPacket(DOWN_BEGIN_WORLD_CHANGE) { _: Unit, _ ->
                println("Started world change")
            }
            .onPacket(DOWN_COMPLETE_WORLD_CHANGE) { _: Unit, _ ->
                println("Complete world change")
            },
        navigator = client.nav
    )
    screen
}

