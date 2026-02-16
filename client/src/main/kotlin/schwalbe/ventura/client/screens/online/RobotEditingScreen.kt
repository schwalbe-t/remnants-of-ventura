
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
import schwalbe.ventura.ROBOT_NAME_MAX_LEN
import schwalbe.ventura.data.ItemCategory
import schwalbe.ventura.engine.ui.Text
import java.io.File
import kotlin.math.atan
import kotlin.math.tan

private fun createBottomPanelButton(text: String, action: () -> Unit) = Stack()
    .add(FlatBackground()
        .withColor(BUTTON_COLOR)
        .withHoverColor(BUTTON_HOVER_COLOR)
    )
    .add(Text()
        .withText(text)
        .withColor(BRIGHT_FONT_COLOR)
        .withSize(75.ph)
        .alignCenter()
        .pad(1.75.vmin)
    )
    .add(ClickArea().withHandler(action))
    .wrapBorderRadius(0.75.vmin)

private fun createRobotInfoSection(): UiElement {
    val topSection: UiSize = 13.vmin
    val logs = Text()
        .withText("Really looooong log\n".repeat(100))
        .withSize(1.3.vmin)
        .withFont(jetbrainsMonoSb())
        .withColor(BRIGHT_FONT_COLOR)
        .wrapScrolling(vert = true, horiz = false)
        .withThumbColor(BUTTON_COLOR)
        .withThumbHoverColor(BUTTON_HOVER_COLOR)
    logs.scrollOffset.target.y = Float.POSITIVE_INFINITY
    return Axis.column()
        .add(topSection, Axis.column(100.ph / 4)
            .add(Text()
                .withText("Running")
                .withColor(RobotStatusDisplay.RUNNING_COLOR)
                .withSize(75.ph)
                .withFont(googleSansSb())
            )
            .add(RobotStatusDisplay.createStatusProp(
                Text().withText("Health:"), Text().withText("33%")
            ))
            .add(RobotStatusDisplay.createStatusProp(
                Text().withText("Memory:"), Text().withText("100%")
            ))
            .add(RobotStatusDisplay.createStatusProp(
                Text().withText("Processor:"), Text().withText("42%")
            ))
            .pad(1.vmin)
            .pad(bottom = 1.vmin)
        )
        .add(100.ph - topSection, logs)
        .pad(1.vmin)
        .withBottomButton("Start/Stop") {
            println("start/stop robot")
        }
}

private fun UiElement.withBottomButton(
    text: String,
    onClick: () -> Unit,
): UiElement {
    val buttonSectionSize: UiSize = 7.vmin
    return Axis.column()
        .add(100.ph - buttonSectionSize, this)
        .add(buttonSectionSize, createBottomPanelButton(text, onClick)
            .pad(left = 1.vmin, right = 1.vmin, bottom = 1.vmin)
        )
}

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
        itemList.add(2.vmin, Text()
            .withText(
                "The 'usercode'-directory in the client directory is empty; " +
                "create a file there, then select it here."
            )
            .withSize(85.ph)
            .withFont(googleSansI())
            .withColor(BRIGHT_FONT_COLOR)
            .pad(left = 2.5.vmin, right = 2.5.vmin)
        )
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

private fun createSelectFileSection(onFileSelect: (String) -> Unit): UiElement {
    val container = Stack()
    listDirectory(File(USERCODE_DIR), listOf(), dest = container, onFileSelect)
    return container
}

private fun createRobotSettingsSection(
    onSetAttachment: ((Item?) -> Unit) -> Unit,
    onAddCodeFile: ((String) -> Unit) -> Unit
): UiElement {
    val subsectionTitleSize: UiSize = 4.5.vmin
    fun subsectionTitle(text: String) = Text()
        .withText(text)
        .withFont(googleSansR())
        .withSize(80.ph)
        .withColor(BRIGHT_FONT_COLOR)
        .pad(1.vmin)
    val topSection: UiSize = 8.vmin
    val attachmentList = Axis.column()
    fun writeAttachments(numAttachments: Int) {
        attachmentList.disposeAll()
        for (i in 0..<numAttachments) {
            addInventoryItem(attachmentList, null, 0) {
                onSetAttachment {
                    println("Selected item ${it?.type?.name} for attachment $i")
                }
            }
        }
        attachmentList.add(50.ph, Space())
    }
    writeAttachments(10)
    val codeFileList = Axis.column()
    fun writeCodeFiles(numCodeFiles: Int) {
        codeFileList.disposeAll()
        fun makeFileButton(
            text: String, alignment: Text.Alignment = Text.Alignment.CENTER,
            onClick: (() -> Unit)? = null
        ): UiElement {
            val root = Stack()
            val bg = FlatBackground().withColor(BUTTON_COLOR)
            root.add(bg)
            root.add(Text()
                .withText(text)
                .withColor(BRIGHT_FONT_COLOR)
                .withSize(75.ph)
                .withAlignment(alignment)
                .pad(1.vmin)
            )
            if (onClick != null) {
                bg.withHoverColor(BUTTON_HOVER_COLOR)
                root.add(ClickArea().withHandler(onClick))
            }
            return root
                .wrapBorderRadius(0.75.vmin)
        }
        for (i in 0..<numCodeFiles) {
            codeFileList.add(4.vmin, Axis.row()
                .add(100.pw - 3 * (100.ph + 1.vmin),
                    makeFileButton("$i.bigton", Text.Alignment.LEFT)
                )
                .add(1.vmin, Space())
                .add(100.ph, makeFileButton("↑") {
                    println("Move file up")
                })
                .add(1.vmin, Space())
                .add(100.ph, makeFileButton("↓") {
                    println("Move file down")
                })
                .add(1.vmin, Space())
                .add(100.ph, makeFileButton("X") {
                    println("Remove file")
                })
                .pad(left = 1.vmin, right = 1.vmin)
            )
            codeFileList.add(1.vmin, Space())
        }
        codeFileList.add(4.vmin, makeFileButton("Add File") {
            onAddCodeFile {
                println("Add code file '$it'")
            }
        }.pad(left = 1.vmin, right = 1.vmin))
        codeFileList.add(50.ph, Space())
    }
    writeCodeFiles(10)
    return Axis.column()
        .add(topSection, Axis.column()
            .add(60.ph, TextInput()
                .withContent(Text()
                    .withFont(googleSansSb())
                    .withColor(BRIGHT_FONT_COLOR)
                    .withSize(75.ph)
                )
                .withValue("Robot Name")
                .let { it.withTypedText { typed ->
                    if (it.value.size < ROBOT_NAME_MAX_LEN) {
                        it.writeText(typed)
                    }
                } }
            )
            .add(40.ph, Text()
                .withText("Robot Model Name")
                .withColor(SECONDARY_BRIGHT_FONT_COLOR)
                .withSize(75.ph)
            )
            .pad(1.5.vmin)
        )
        .add(100.ph - topSection, Axis.column(100.ph / 2)
            .add(Axis.column()
                .add(subsectionTitleSize, subsectionTitle("Attachments"))
                .add(100.ph - subsectionTitleSize, attachmentList
                    .wrapScrolling()
                    .withThumbColor(BUTTON_COLOR)
                    .withThumbHoverColor(BUTTON_HOVER_COLOR)
                    .pad(bottom = 2.vmin)
                )
            )
            .add(Axis.column()
                .add(subsectionTitleSize, subsectionTitle("Code Files"))
                .add(100.ph - subsectionTitleSize, codeFileList
                    .wrapScrolling()
                    .withThumbColor(BUTTON_COLOR)
                    .withThumbHoverColor(BUTTON_HOVER_COLOR)
                    .pad(bottom = 2.vmin)
                )
            )
        )
        .pad(1.vmin)
        .withBottomButton("Delete Robot") {
            println("on robot delete")
        }
}

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
            world.player.assertAnimation(PlayerAnim.squat, 0.5f)
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
    val rhs = Stack()
    fun resetRhs() {
        rhs.disposeAll()
        rhs.add(createRobotInfoSection())
    }
    resetRhs()
    fun onSetAttachment(onItemSelected: (Item?) -> Unit) {
        val itemSelect = createItemListSection(
            client, packets,
            displayedEntries = { i, _ -> i.type.category.isRobotAttachment },
            onItemSelect = { i, _ ->
                onItemSelected(i)
                resetRhs()
            }
        )
        rhs.disposeAll()
        rhs.add(itemSelect
            .withBottomButton("Remove Attachment") {
                onItemSelected(null)
                resetRhs()
            }
            .withBottomButton("Cancel") { resetRhs() }
        )
    }
    fun onAddCodeFile(onFileSelected: (String) -> Unit) {
        val fileSelect = createSelectFileSection { path ->
            onFileSelected(path)
            resetRhs()
        }
        rhs.disposeAll()
        rhs.add(fileSelect
            .withBottomButton("Cancel") { resetRhs() }
        )
    }
    screen.add(element = Axis.row()
        .add(66.6.vw, Stack()
            .add(background)
            .add(FlatBackground().withColor(PANEL_BACKGROUND))
            .add(Axis.row()
                .add(50.pw, createRobotSettingsSection(
                    ::onSetAttachment,
                    ::onAddCodeFile
                ))
                .add(50.pw, rhs)
            )
        )
    )
    screen
}
