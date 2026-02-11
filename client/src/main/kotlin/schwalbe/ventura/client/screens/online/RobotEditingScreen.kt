
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.data.Item
import schwalbe.ventura.net.PacketHandler
import org.joml.Vector3f
import schwalbe.ventura.client.LocalKeys.TITLE_INVENTORY
import kotlin.math.atan
import kotlin.math.tan

private fun createRobotInfoSection(): UiElement = Space()

private fun cancellableSection(
    section: UiElement, onCancel: () -> Unit
): UiElement = Axis.column()
    .add(100.ph - 8.vmin, section)
    .add(8.vmin, createButton(
        content = Text()
            .withText("Cancel")
            .withColor(BRIGHT_FONT_COLOR)
            .withSize(75.ph)
            .alignCenter()
            .pad(2.vmin),
        handler = onCancel
    ).pad(1.vmin))

private fun createSelectItemSection(
    client: Client, packetHandler: PacketHandler<Unit>,
    onItemSelect: (Pair<Item, Int>?) -> Unit
): UiElement = cancellableSection(
    section = createItemListSection(
        client, packetHandler,
        onItemSelect = { item, count -> onItemSelect(Pair(item, count)) }
    ),
    onCancel = { onItemSelect(null) }
)

private fun createSelectFileSection(): UiElement {
    val container = Stack()
    fun listDirectory(absDir: String, relDir: List<String>) {
        container.disposeAll()
        container.add(Axis.column()
            .add(8.vmin, Axis.column()
                .add(3.vmin, Text()
                    .withText("Select Code File")
                    .withColor(BRIGHT_FONT_COLOR)
                    .withFont(googleSansSb())
                    .withSize(85.ph)
                )
                .add(2.vmin, Text()
                    .withText(relDir.joinToString(separator = " > "))
                    .withColor(BRIGHT_FONT_COLOR)
                    .withFont(jetbrainsMonoSb())
                    .withSize(85.ph)
                )
                .pad(2.5.vmin)
            )
            .add(100.ph - 8.vmin, Space())
        )
    }
    listDirectory(USERCODE_DIR, listOf())
    return container
}

private fun createRobotSettingsSection(): UiElement = Space()

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
    val packets = PacketHandler.receiveDownPackets<Unit>()
        .addErrorLogging()
        .addWorldHandling(client)
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
        packets = packets,
        navigator = client.nav
    )
    screen.add(Axis.row()
        .add(66.6.vw, Stack()
            .add(background)
            .add(FlatBackground().withColor(PANEL_BACKGROUND))
            .add(Axis.row()
                .add(50.pw, Space())
                .add(50.pw, createSelectItemSection(
                    client, packets, onItemSelect = {
                        println("Selected item")
                    }
                ))
            )
        )
    )
    screen
}
