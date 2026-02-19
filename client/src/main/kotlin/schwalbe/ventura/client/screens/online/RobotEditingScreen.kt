
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.net.*
import schwalbe.ventura.data.Item
import schwalbe.ventura.ROBOT_NAME_MAX_LEN
import java.io.File
import org.joml.Vector3f
import kotlin.math.atan
import kotlin.math.roundToInt
import kotlin.math.tan
import kotlin.uuid.Uuid

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

private fun createRobotInfoSection(
    client: Client, packetHandler: PacketHandler<Unit>, robotId: Uuid
): UiElement {
    val l = localized()
    val topSection: UiSize = 13.vmin
    val container = Axis.column()
    val statusText = Text()
    val healthValueText = Text()
    val memoryValueText = Text()
    val processorValueText = Text()
    packetHandler.onPacketRef(PacketType.WORLD_STATE) { ws, _, phr ->
        if (container.wasDisposed) {
            packetHandler.remove(phr)
            return@onPacketRef
        }
        val pi = ws.ownedRobots[robotId] ?: return@onPacketRef
        val si = ws.allRobots[robotId] ?: return@onPacketRef
        statusText.withText(l[si.status.localNameKey])
        statusText.withColor(si.status.displayColor)
        val pHealth: Int = (pi.fracHealth * 100f).roundToInt()
        val pMemUsage: Int = (pi.fracMemUsage * 100f).roundToInt()
        val pCpuUsage: Int = (pi.fracCpuUsage * 100f).roundToInt()
        healthValueText.withText("$pHealth%")
        memoryValueText.withText("$pMemUsage%")
        processorValueText.withText("$pCpuUsage%")
    }
    val logText = Text()
    packetHandler.onPacketRef(PacketType.ROBOT_LOGS) { logs, _, phr ->
        if (logText.wasDisposed) {
            packetHandler.remove(phr)
            return@onPacketRef
        }
        logText.withText(logs.logs)
    }
    client.network.outPackets?.send(Packet.serialize(
        PacketType.REQUEST_ROBOT_LOGS, robotId
    ))
    return container
        .add(topSection, Axis.column(100.ph / 4)
            .add(statusText
                .withSize(75.ph)
                .withFont(googleSansSb())
            )
            .add(RobotStatusDisplay.createStatusProp(
                Text().withText(l[LABEL_ROBOT_STAT_HEALTH]), healthValueText
            ))
            .add(RobotStatusDisplay.createStatusProp(
                Text().withText(l[LABEL_ROBOT_STAT_MEMORY]), memoryValueText
            ))
            .add(RobotStatusDisplay.createStatusProp(
                Text().withText(l[LABEL_ROBOT_STAT_PROCESSOR]),
                processorValueText
            ))
            .pad(1.vmin)
            .pad(bottom = 1.vmin)
        )
        .add(100.ph - topSection, logText
            .withSize(1.3.vmin)
            .withFont(jetbrainsMonoSb())
            .withColor(BRIGHT_FONT_COLOR)
            .wrapScrolling(vert = true, horiz = false)
            .withThumbColor(BUTTON_COLOR)
            .withThumbHoverColor(BUTTON_HOVER_COLOR)
            .withStickToBottom()
        )
        .pad(1.vmin)
        .withBottomButton(l[BUTTON_ROBOT_START]) { // TODO! or 'BUTTON_ROBOT_STOP'
            println("start/stop robot") // TODO! send start/stop commands
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
            .withText(localized()[PLACEHOLDER_NO_SOURCE_FILES])
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
                .withText(localized()[TITLE_SELECT_SOURCE_FILE])
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
    client: Client, robotId: Uuid, initialState: SharedRobotInfo?,
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
    val l = localized()
    val topSection: UiSize = 8.vmin
    val attachmentList = Axis.column()
    fun writeAttachments(numAttachments: Int) {
        attachmentList.disposeAll()
        for (i in 0..<numAttachments) {
            // TODO! get robot attachments and display instead of 'null'
            addInventoryItem(attachmentList, null, 0) {
                onSetAttachment {
                    // TODO! send request to set attachment
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
            // TODO! get actual file names
            codeFileList.add(4.vmin, Axis.row()
                .add(100.pw - 3 * (100.ph + 1.vmin),
                    // TODO! actual file name
                    makeFileButton("$i.bigton", Text.Alignment.LEFT)
                )
                .add(1.vmin, Space())
                .add(100.ph, makeFileButton("↑") {
                    // TODO! implement
                    println("Move file up")
                })
                .add(1.vmin, Space())
                .add(100.ph, makeFileButton("↓") {
                    // TODO! implement
                    println("Move file down")
                })
                .add(1.vmin, Space())
                .add(100.ph, makeFileButton("X") {
                    // TODO! implement
                    println("Remove file")
                })
                .pad(left = 1.vmin, right = 1.vmin)
            )
            codeFileList.add(1.vmin, Space())
        }
        codeFileList.add(4.vmin, makeFileButton(l[BUTTON_ADD_CODE_FILE]) {
            onAddCodeFile {
                // TODO! add code file to robot
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
                .withValue(initialState?.name ?: "")
                .let { it.withTypedText { typed ->
                    if (it.value.size < ROBOT_NAME_MAX_LEN) {
                        it.writeText(typed)
                    }
                } }
                .withValueChangedHandler { value ->
                    client.network.outPackets?.send(Packet.serialize(
                        PacketType.SET_ROBOT_NAME,
                        RobotNameChangePacket(robotId, value)
                    ))
                }
            )
            .add(40.ph, Text()
                .withText(
                    initialState?.item?.type?.localNameKey?.let(l::get) ?: ""
                )
                .withColor(SECONDARY_BRIGHT_FONT_COLOR)
                .withSize(75.ph)
            )
            .pad(1.5.vmin)
        )
        .add(100.ph - topSection, Axis.column(100.ph / 2)
            .add(Axis.column()
                .add(subsectionTitleSize,
                    subsectionTitle(l[TITLE_ROBOT_ATTACHMENTS])
                )
                .add(100.ph - subsectionTitleSize, attachmentList
                    .wrapScrolling()
                    .withThumbColor(BUTTON_COLOR)
                    .withThumbHoverColor(BUTTON_HOVER_COLOR)
                    .pad(bottom = 2.vmin)
                )
            )
            .add(Axis.column()
                .add(subsectionTitleSize,
                    subsectionTitle(l[TITLE_ROBOT_CODE_FILES])
                )
                .add(100.ph - subsectionTitleSize, codeFileList
                    .wrapScrolling()
                    .withThumbColor(BUTTON_COLOR)
                    .withThumbHoverColor(BUTTON_HOVER_COLOR)
                    .pad(bottom = 2.vmin)
                )
            )
        )
        .pad(1.vmin)
        .withBottomButton(l[BUTTON_DESTROY_ROBOT]) {
            client.network.outPackets?.send(Packet.serialize(
                PacketType.DESTROY_ROBOT, robotId
            ))
            client.nav.pop()
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

fun robotEditingScreen(client: Client, robotId: Uuid): () -> GameScreen = {
    val background = BlurBackground()
        .withRadius(3)
        .withSpread(5)
    val packets = PacketHandler.receiveDownPackets<Unit>()
        .addErrorLogging()
        .addWorldHandling(client)
    val sharedRobotInfo: SharedRobotInfo?
        = client.world?.state?.lastReceived?.allRobots[robotId]
    val screen = GameScreen(
        onOpen = {
            client.world?.camController?.mode = PLAYER_IN_RIGHT_THIRD
        },
        render = render@{
            if (Key.ESCAPE.wasPressed || Key.E.wasPressed) {
                client.nav.pop()
            }
            val world = client.world ?: return@render
            world.update(client, captureInput = false)
            if (sharedRobotInfo != null) {
                val toRobot = sharedRobotInfo.position.toVector3f()
                    .sub(world.player.position)
                if (toRobot.lengthSquared() != 0f) {
                    world.player.rotateAlong(toRobot)
                }
            }
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
        rhs.add(createRobotInfoSection(client, packets, robotId))
    }
    resetRhs()
    fun onSetAttachment(onItemSelected: (Item?) -> Unit) {
        rhs.disposeAll()
        rhs.add(createItemListSection(
            packets,
            displayedEntries = { i, _ -> i.type.category.isRobotAttachment },
            onItemSelect = { i, _ ->
                onItemSelected(i)
                resetRhs()
            }
        ))
        requestInventoryContents(client)
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
                    client, robotId, sharedRobotInfo,
                    ::onSetAttachment,
                    ::onAddCodeFile
                ))
                .add(50.pw, rhs)
            )
        )
    )
    screen
}
