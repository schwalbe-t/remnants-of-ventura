
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

private fun createBottomPanelButton(text: Text, action: () -> Unit) = Stack()
    .add(FlatBackground()
        .withColor(BUTTON_COLOR)
        .withHoverColor(BUTTON_HOVER_COLOR)
    )
    .add(text
        .withColor(BRIGHT_FONT_COLOR)
        .withSize(75.ph)
        .alignCenter()
        .pad(1.75.vmin)
    )
    .add(ClickArea().withHandler(action))
    .wrapBorderRadius(0.75.vmin)

private data class BottomButtonHandler(var impl: () -> Unit = {})

private fun UiElement.withBottomButton(
    text: Text,
    onClick: BottomButtonHandler
): UiElement {
    val buttonSectionSize: UiSize = 7.vmin
    return Axis.column()
        .add(100.ph - buttonSectionSize, this)
        .add(buttonSectionSize, createBottomPanelButton(text) { onClick.impl() }
            .pad(left = 1.vmin, right = 1.vmin, bottom = 1.vmin)
        )
}

private fun UiElement.withBottomButton(
    text: String,
    onClick: () -> Unit,
): UiElement = this.withBottomButton(
    Text().withText(text),
    BottomButtonHandler(onClick)
)

private fun createRobotInfoSection(
    client: Client, packetHandler: PacketHandler<Unit>, robotId: Uuid
): UiElement {
    val l = localized()
    val topSection: UiSize = 13.vmin
    val container = Axis.column()
    val statusText = Text()
    val toggleButtonText = Text()
    val toggleButtonHandler = BottomButtonHandler()
    val healthValueText = Text()
    val memoryValueText = Text()
    val processorValueText = Text()
    packetHandler.onPacketUntil(
        PacketType.WORLD_STATE, until = container::wasDisposed
    ) { ws, _ ->
        val pi = ws.ownedRobots[robotId] ?: return@onPacketUntil
        val si = ws.allRobots[robotId] ?: return@onPacketUntil
        statusText.withText(l[si.status.localNameKey])
        statusText.withColor(si.status.displayColor)
        val toggleLabel =
            if (!si.status.isRunning) { BUTTON_ROBOT_START }
            else { BUTTON_ROBOT_STOP }
        toggleButtonText.withText(l[toggleLabel])
        toggleButtonHandler.impl = {
            fun sendAction(t: PacketType<Uuid>)
                = client.network.outPackets?.send(Packet.serialize(t, robotId))
            if (si.status.isRunning) {
                sendAction(PacketType.STOP_ROBOT)
            } else {
                sendAction(PacketType.START_ROBOT)
                sendAction(PacketType.PAUSE_ROBOT)
            }
        }
        val pHealth: Int = (pi.fracHealth * 100f).roundToInt()
        val pMemUsage: Int = (pi.fracMemUsage * 100f).roundToInt()
        val pCpuUsage: Int = (pi.fracCpuUsage * 100f).roundToInt()
        healthValueText.withText("$pHealth%")
        memoryValueText.withText("$pMemUsage%")
        processorValueText.withText("$pCpuUsage%")
    }
    val logText = Text()
    packetHandler.onPacketUntil(
        PacketType.ROBOT_LOGS, until = logText::wasDisposed
    ) { logs, _ ->
        logText.withText(logs.logs)
    }
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
        .withBottomButton(toggleButtonText, toggleButtonHandler)
}

fun requestRobotLogs(client: Client, robotId: Uuid) {
    client.network.outPackets?.send(Packet.serialize(
        PacketType.REQUEST_ROBOT_LOGS, robotId
    ))
}

private fun listDirectory(
    rootDir: File, dir: File, relDir: List<String>,
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
                rootDir, dir.parentFile, relDir.subList(0, relDir.size - 1),
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
                    return@handler onFileSelect(file.relativeTo(rootDir).path)
                }
                listDirectory(
                    rootDir, dir.resolve(file.name), relDir + file.name,
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
    val rootDir = File(USERCODE_DIR)
    listDirectory(rootDir, rootDir, listOf(), dest = container, onFileSelect)
    return container
}

private fun createRobotSettingsSection(
    client: Client, packetHandler: PacketHandler<Unit>,
    robotId: Uuid, initialState: SharedRobotInfo?,
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
    var displayedAttachments: List<Item?>? = null
    packetHandler.onPacketUntil(
        PacketType.WORLD_STATE, until = attachmentList::wasDisposed
    ) { ws, _ ->
        val pi = ws.ownedRobots[robotId] ?: return@onPacketUntil
        if (pi.attachments == displayedAttachments) { return@onPacketUntil }
        attachmentList.disposeAll()
        for (i in pi.attachments.indices) {
            val attachedItem: Item? = pi.attachments[i]
            val attachedCount: Int = if (attachedItem != null) { 1 } else { 0 }
            addInventoryItem(attachmentList, attachedItem, attachedCount) {
                onSetAttachment { selectedItem ->
                    client.network.outPackets?.send(Packet.serialize(
                        PacketType.SET_ROBOT_ATTACHMENT,
                        RobotAttachmentChangePacket(
                            robotId, attachmentId = i, selectedItem
                        )
                    ))
                }
            }
        }
        attachmentList.add(50.ph, Space())
        displayedAttachments = pi.attachments
    }
    val codeFileList = Axis.column()
    var displayedSourceFiles: List<String>? = null
    packetHandler.onPacketUntil(
        PacketType.WORLD_STATE, until = codeFileList::wasDisposed
    ) { ws, _ ->
        val pi = ws.ownedRobots[robotId] ?: return@onPacketUntil
        if (pi.sourceFiles == displayedSourceFiles) { return@onPacketUntil }
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
        val sourceFiles: MutableList<String> = pi.sourceFiles.toMutableList()
        fun sendChangedSourceFiles() {
            client.network.outPackets?.send(Packet.serialize(
                PacketType.SET_ROBOT_SOURCES,
                RobotSourceFilesChangePacket(robotId, sourceFiles)
            ))
        }
        fun wrapFileIdx(i: Int): Int {
            if (i < 0) { return i + sourceFiles.size }
            return i % sourceFiles.size
        }
        fun swapFiles(aIdx: Int, bIdx: Int) {
            val a = sourceFiles[aIdx]
            sourceFiles[aIdx] = sourceFiles[bIdx]
            sourceFiles[bIdx] = a
        }
        codeFileList.disposeAll()
        for (i in pi.sourceFiles.indices) {
            val codeFilePath = pi.sourceFiles[i]
            codeFileList.add(4.vmin, Axis.row()
                .add(100.pw - 3 * (100.ph + 1.vmin),
                    makeFileButton(codeFilePath, Text.Alignment.LEFT)
                )
                .add(1.vmin, Space())
                .add(100.ph, makeFileButton("↑") {
                    swapFiles(i, wrapFileIdx(i - 1))
                    sendChangedSourceFiles()
                })
                .add(1.vmin, Space())
                .add(100.ph, makeFileButton("↓") {
                    swapFiles(i, wrapFileIdx(i + 1))
                    sendChangedSourceFiles()
                })
                .add(1.vmin, Space())
                .add(100.ph, makeFileButton("X") {
                    sourceFiles.removeAt(i)
                    sendChangedSourceFiles()
                })
                .pad(left = 1.vmin, right = 1.vmin)
            )
            codeFileList.add(1.vmin, Space())
        }
        codeFileList.add(4.vmin, makeFileButton(l[BUTTON_ADD_CODE_FILE]) {
            onAddCodeFile {
                sourceFiles.add(it)
                sendChangedSourceFiles()
            }
        }.pad(left = 1.vmin, right = 1.vmin))
        codeFileList.add(50.ph, Space())
        displayedSourceFiles = pi.sourceFiles
    }
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

private const val LOG_REQUEST_INTERVAL: Long = 1000L

fun robotEditingScreen(client: Client, robotId: Uuid): () -> GameScreen = {
    val background = BlurBackground()
        .withRadius(3)
        .withSpread(5)
    val packets = PacketHandler.receiveDownPackets<Unit>()
        .addErrorLogging()
        .addWorldHandling(client)
        .updateStoredSources(client)
    val sharedRobotInfo: SharedRobotInfo?
        = client.world?.state?.lastReceived?.allRobots[robotId]
    var lastLogRequestTime: Long = 0
    val screen = GameScreen(
        onOpen = {
            client.world?.camController?.mode = PLAYER_IN_RIGHT_THIRD
            client.network.outPackets?.send(Packet.serialize(
                PacketType.PAUSE_ROBOT, robotId
            ))
        },
        onClose = {
            client.network.outPackets?.send(Packet.serialize(
                PacketType.UNPAUSE_ROBOT, robotId
            ))
        },
        render = render@{
            val now: Long = System.currentTimeMillis()
            if (lastLogRequestTime + LOG_REQUEST_INTERVAL <= now) {
                requestRobotLogs(client, robotId)
                lastLogRequestTime = now
            }
            if (Key.ESCAPE.wasPressed || Key.E.wasPressed) {
                client.nav.pop()
            }
            SourceFiles.update(client)
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
    val l = localized()
    val rhs = Stack()
    fun resetRhs() {
        rhs.disposeAll()
        rhs.add(createRobotInfoSection(client, packets, robotId))
    }
    resetRhs()
    fun onSetAttachment(onItemSelected: (Item?) -> Unit) {
        rhs.disposeAll()
        val itemList = createItemListSection(
            packets,
            displayedEntries = { i, _ -> i.type.category.isRobotAttachment },
            onItemSelect = { i, _ ->
                onItemSelected(i)
                resetRhs()
            }
        )
        rhs.add(itemList
            .withBottomButton(l[BUTTON_REMOVE_ITEM]) {
                onItemSelected(null)
                resetRhs()
            }
            .withBottomButton(l[BUTTON_CANCEL_EDIT]) { resetRhs() }
        )
        requestInventoryContents(client)
    }
    fun onAddCodeFile(onFileSelected: (String) -> Unit) {
        val fileSelect = createSelectFileSection { path ->
            onFileSelected(path)
            resetRhs()
        }
        rhs.disposeAll()
        rhs.add(fileSelect
            .withBottomButton(l[BUTTON_CANCEL_EDIT]) { resetRhs() }
        )
    }
    screen.add(element = Axis.row()
        .add(66.6.vw, Stack()
            .add(background)
            .add(FlatBackground().withColor(PANEL_BACKGROUND))
            .add(Axis.row()
                .add(50.pw, createRobotSettingsSection(
                    client, packets, robotId, sharedRobotInfo,
                    ::onSetAttachment,
                    ::onAddCodeFile
                ))
                .add(50.pw, rhs)
            )
        )
    )
    screen
}
