
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.net.PacketHandler

fun onlineSettingsScreen(client: Client): () -> GameScreen = {
    settingsScreen(
        client,
        renderBackground = { root ->
            if (Key.ESCAPE.wasPressed) {
                client.nav.pop()
            }
            PausedScreen.updateBackground(
                client,
                playerFollowCursor = true, playerAnim = PlayerAnim.thinking
            )
            root.invalidate()
        },
        networkState = keepNetworkConnectionAlive(client, onFail = { reason ->
            client.nav.replace(serverConnectionFailedScreen(reason, client))
            client.network.clearError()
        }),
        packets = PacketHandler.receiveDownPackets<Unit>()
            .addErrorLogging()
            .addWorldHandling(client)
            .updateStoredSources(client)
            .addChatMessageHandling(client),
    )()
}

