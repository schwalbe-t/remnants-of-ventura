
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectingScreen
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.net.Packet
import schwalbe.ventura.net.PacketHandler
import schwalbe.ventura.net.PacketType

private val OPTION_HEIGHT: UiSize = 6.5.vmin
private val OPTION_PADDING: UiSize = 1.vmin

private fun addOption(
    options: Axis, name: String, action: () -> Unit
) {
    options.add(OPTION_HEIGHT + OPTION_PADDING, Stack()
        .add(FlatBackground()
            .withColor(Theme.BUTTON_COLOR)
            .withHoverColor(Theme.BUTTON_HOVER_COLOR)
        )
        .add(Text()
            .withText(name)
            .withSize(75.ph)
            .pad(2.vmin)
        )
        .add(ClickArea().withLeftHandler(action))
        .wrapBorderRadius(0.75.vmin)
        .pad(bottom = 1.vmin)
    )
}

fun escapeMenuScreen(client: Client): () -> GameScreen = {
    val world: World? = client.world
    val background = BlurBackground()
        .withRadius(3)
        .withSpread(5)
    val screen = GameScreen(
        onOpen = {
            client.world?.let {
                it.camController.mode = CameraModes.playerInRightHalf(it.player)
            }
        },
        render = {
            if (Key.ESCAPE.wasPressed) {
                client.nav.pop()
            }
            SourceFiles.update(client)
            client.world?.update(client, captureInput = false)
            client.world?.player?.facePoint(
                client.renderer.camera
                    .castRay(client.renderer.dest, Mouse.position)
                    .afterDistance(7.5f)
            )
            client.world?.player?.assertAnimation(PlayerAnim.thinking)
            client.world?.render(client)
            background.invalidate()
        },
        networkState = keepNetworkConnectionAlive(client, onFail = { reason ->
            client.nav.replace(serverConnectionFailedScreen(reason, client))
            client.network.clearError()
        }),
        packets = PacketHandler.receiveDownPackets<Unit>()
            .addErrorLogging()
            .addWorldHandling(client)
            .updateStoredSources(client),
        navigator = client.nav
    )
    val options = Axis.column()
    addOption(options, localized()[BUTTON_BACK_TO_GAME], action = {
        client.nav.pop()
    })
    if (world != null && !world.isMain) {
        addOption(options, localized()[BUTTON_LEAVE_INTERIOR], action = {
            client.network.outPackets?.send(Packet.serialize(
                PacketType.REQUEST_WORLD_LEAVE, Unit
            ))
            client.nav.pop()
        })
    }
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
    val areaHeight: UiSize = options.children.size * OPTION_HEIGHT +
        (options.children.size - 1) * OPTION_PADDING
    screen.add(Axis.row()
        .add(50.vw, Stack()
            .add(background)
            .add(FlatBackground().withColor(Theme.PANEL_BACKGROUND))
            .add(Axis.column()
                .add(50.ph - (areaHeight / 2), Space())
                .add(areaHeight, options
                    .pad(left = 5.vmin, right = 5.vmin)
                )
                .add(50.ph - (areaHeight / 2), Space())
            )
        )
    )
    screen
}
