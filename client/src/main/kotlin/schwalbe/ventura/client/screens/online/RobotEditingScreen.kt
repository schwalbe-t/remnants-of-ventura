
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
import schwalbe.ventura.engine.ui.Text
import java.io.File
import kotlin.math.atan
import kotlin.math.tan

private fun createRobotInfoSection(): UiElement = Space()

private fun cancellableSection(
    section: UiElement, onCancel: () -> Unit
): UiElement = Axis.column()
    .add(100.ph - 8.vmin, section)
    .add(7.5.vmin, createButton(
        content = Text()
            .withText("Cancel")
            .withColor(BRIGHT_FONT_COLOR)
            .withSize(75.ph)
            .alignCenter()
            .pad(1.75.vmin),
        handler = onCancel
    ).pad(left = 25.pw, right = 25.pw, top = 1.vmin, bottom = 1.vmin))

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

private fun listDirectory(
    dir: File, relDir: List<String>,
    dest: Stack, onFileSelect: (String) -> Unit
) {
    val itemList = Axis.column()
    fun addFileItem(
        name: String, bold: Boolean, dark: Boolean, handler: () -> Unit
    ) {
        val font = if (bold) googleSansSb() else googleSansR()
        val color = if (dark) SECONDARY_BRIGHT_FONT_COLOR else BRIGHT_FONT_COLOR
        itemList.add(5.vmin, Stack()
            .add(FlatBackground()
                .withColor(BUTTON_COLOR)
                .withHoverColor(BUTTON_HOVER_COLOR)
            )
            .add(Text()
                .withText(name)
                .withColor(color)
                .withFont(font)
                .withSize(75.ph)
                .pad(1.vmin)
            )
            .add(ClickArea().withHandler(handler))
            .wrapBorderRadius(0.75.vmin)
            .pad(left = 1.vmin, right = 1.vmin, bottom = 1.vmin)
        )
    }
    val dirContents: Array<File> = dir.listFiles() ?: arrayOf()
    if (relDir.isEmpty() && dirContents.isEmpty()) {
        // TODO! placeholder
    }
    if (relDir.isNotEmpty()) {
        addFileItem(
            name = "..",
            bold = true, dark = true,
            handler = { listDirectory(
                dir.parentFile, relDir.subList(0, relDir.size - 1),
                dest, onFileSelect
            ) }
        )
    }
    for (file in dirContents.sorted()) {
        val isDir: Boolean = file.isDirectory
        val symbol: String = if (isDir) "> " else ""
        addFileItem(
            name = symbol + file.name,
            bold = isDir, dark = false,
            handler = handler@{
                if (!isDir) {
                    return@handler onFileSelect(file.absolutePath)
                }
                listDirectory(
                    dir.resolve(file.name), relDir + file.name,
                    dest, onFileSelect
                )
            }
        )
    }
    dest.disposeAll()
    dest.add(Axis.column()
        .add(8.vmin, Axis.column()
            .add(60.ph, Text()
                .withText("Select Code File")
                .withColor(BRIGHT_FONT_COLOR)
                .withFont(googleSansSb())
                .withSize(85.ph)
            )
            .add(40.ph, Text()
                .withText(">" + relDir.joinToString(
                    transform = { " $it >" },
                    separator = ""
                ))
                .withColor(SECONDARY_BRIGHT_FONT_COLOR)
                .withFont(jetbrainsMonoSb())
                .withSize(85.ph)
            )
            .pad(1.5.vmin)
        )
        .add(100.ph - 8.vmin, itemList
            .wrapScrolling()
            .withThumbColor(BUTTON_COLOR)
            .withThumbHoverColor(BUTTON_HOVER_COLOR)
        )
    )
}

private fun createSelectFileSection(onFileSelect: (String?) -> Unit): UiElement {
    val container = Stack()
    listDirectory(File(USERCODE_DIR), listOf(), dest = container, onFileSelect)
    return cancellableSection(container, onCancel = { onFileSelect(null) })
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
                .add(50.pw, createRobotSettingsSection())
//                .add(50.pw, createSelectItemSection(
//                    client, packets, onItemSelect = {
//                        println("Selected item")
//                    }
//                ))
//                .add(50.pw, createSelectFileSection {
//                    println("Selected '$it'")
//                })
                .add(50.pw, createRobotInfoSection())
            )
        )
    )
    screen
}
