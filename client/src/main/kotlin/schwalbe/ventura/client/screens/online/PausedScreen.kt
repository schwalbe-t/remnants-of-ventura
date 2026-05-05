
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.game.update
import schwalbe.ventura.client.screens.GameScreen
import schwalbe.ventura.client.screens.keepNetworkConnectionAlive
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.engine.gfx.AnimationRef
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.engine.ui.BlurBackground
import schwalbe.ventura.net.PacketHandler
import kotlin.Boolean

class PausedScreen(
    client: Client,
    camMode: (World) -> CameraController.Mode,
    closeIf: () -> Boolean = { Key.ESCAPE.wasPressed },
    captureInput: Boolean = false,
    playerFollowCursor: Boolean = true,
    playerAnim: AnimationRef<PersonAnim>?,
    onOpen: () -> Unit = {},
    render: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    companion object {
        fun updateBackground(
            client: Client,
            captureInput: Boolean = false,
            playerFollowCursor: Boolean = true,
            playerAnim: AnimationRef<PersonAnim>?
        ) {
            SourceFiles.update(client)
            val world: World = client.world ?: return
            world.update(client, captureInput)
            if (playerFollowCursor) {
                val cursorWorld = client.renderer.camera
                    .castRay(client.renderer.dest, Mouse.position)
                    .afterDistance(7.5f)
                world.player.facePoint(cursorWorld)
            }
            playerAnim?.let {
                world.player.assertAnimation(it)
            }
            world.render(client)
        }
    }

    val packets = PacketHandler.receiveDownPackets<Unit>()
        .addErrorLogging()
        .addWorldHandling(client)
        .updateStoredSources(client)
        .addChatMessageHandling(client)
    val background = BlurBackground()
        .withRadius(3)
        .withSpread(5)
    val screen = GameScreen(
        onOpen = {
            client.world?.let { it.camController.mode = camMode(it) }
            onOpen()
        },
        render = {
            if (closeIf()) {
                client.nav.pop()
            }
            updateBackground(
                client, captureInput, playerFollowCursor, playerAnim
            )
            this.background.invalidate()
            render()
        },
        onClose = onClose,
        networkState = keepNetworkConnectionAlive(client, onFail = { reason ->
            client.nav.replace(serverConnectionFailedScreen(reason, client))
            client.network.clearError()
        }),
        packets = packets,
        navigator = client.nav
    )
}
