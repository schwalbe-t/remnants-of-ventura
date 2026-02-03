
package schwalbe.ventura.client.screens.online

import org.joml.Vector3f
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectingScreen
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.net.PacketHandler
import kotlin.math.atan
import kotlin.math.tan

private fun addOption(
    options: Axis, name: String, action: () -> Unit
) {
    options.add(7.5.vmin, Stack()
        .add(FlatBackground()
            .withColor(BUTTON_COLOR)
            .withHoverColor(BUTTON_HOVER_COLOR)
        )
        .add(Text()
            .withText(name)
            .withColor(BRIGHT_FONT_COLOR)
            .withSize(80.ph)
            .pad(2.vmin)
        )
        .add(ClickArea().withHandler(action))
        .wrapBorderRadius(0.75.vmin)
        .pad(bottom = 1.vmin)
    )
}

val PLAYER_IN_RIGHT_HALF = CameraController.Mode(
    lookAt = { _, w, _ -> Vector3f()
        .add(w.player.position)
        .add(0f, +1.5f, 0f)
    },
    fovDegrees = 20f,
    offsetAngleX = { _, hh, _ -> atan(tan(hh) * -1f/2f) },
    distance = { _ -> 10f }
)

fun escapeMenuScreen(client: Client): () -> GameScreen = {
    client.world?.camController?.mode = PLAYER_IN_RIGHT_HALF
    val background = BlurBackground()
        .withRadius(3)
        .withSpread(5)
    val screen = GameScreen(
        render = {
            if (Key.ESCAPE.wasPressed) {
                client.nav.pop()
            }
            client.world?.update(client, captureInput = false)
            client.world?.render(client)
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
    val areaSize: UiSize = (3 * 7.5 + 2).vmin
    val options = Axis.column()
    addOption(options, localized()[BUTTON_BACK_TO_GAME], action = {
        client.nav.pop()
    })
    addOption(options, localized()[BUTTON_LOG_OUT], action = {
        val s: NetworkClient.State = client.network.state
        if (s !is NetworkClient.Connected) { return@addOption }
        client.nav.pop()
        client.nav.replace(serverConnectingScreen(s.address, s.port, client))
        client.config.sessions.remove("${s.address}:${s.port}")
        client.config.write()
    })
    addOption(options, localized()[BUTTON_DISCONNECT], action = {
        client.nav.pop()
        client.nav.pop()
    })
    screen.add(Axis.row()
        .add(50.vw, Stack()
            .add(background)
            .add(FlatBackground().withColor(PANEL_BACKGROUND))
            .add(Axis.column()
                .add(50.ph - (areaSize / 2), Space())
                .add(areaSize, options
                    .pad(left = 5.vmin, right = 5.vmin)
                )
                .add(50.ph - (areaSize / 2), Space())
            )
        )
    )
    screen
}
