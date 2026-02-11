
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.net.PacketHandler
import org.joml.Vector3f
import kotlin.math.atan
import kotlin.math.tan

private val PLAYER_IN_RIGHT_THIRD = CameraController.Mode(
    lookAt = { _, w, _ -> Vector3f()
        .add(w.player.position)
        .add(0f, +1.25f, 0f)
    },
    fovDegrees = 20f,
    offsetAngleX = { _, hh, _ -> atan(tan(hh) * -2f/3f) },
    distance = { _ -> 10f }
)

fun robotEditingScreen(client: Client): () -> GameScreen = {
    client.world?.camController?.mode = PLAYER_IN_RIGHT_THIRD
    val background = BlurBackground()
        .withRadius(3)
        .withSpread(5)
    val screen = GameScreen(
        render = render@{
            if (Key.ESCAPE.wasPressed || Key.E.wasPressed) {
                client.nav.pop()
            }
            val world = client.world ?: return@render
            world.update(client, captureInput = false)
            world.player.rotateAlong(
                // TODO! exchange for rotation towards robot instead of origin
                Vector3f(0f, 0f, 0f).sub(world.player.position)
            )
            if (world.player.anim.latestAnim != PlayerAnim.squat) {
                world.player.anim.transitionTo(PlayerAnim.squat, 0.5f)
            }
            world.render(client)
            background.invalidate()
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
    screen.add(Axis.row()
        .add(66.6.vw, Stack()
            .add(background)
            .add(FlatBackground().withColor(PANEL_BACKGROUND))
        )
    )
    screen
}
