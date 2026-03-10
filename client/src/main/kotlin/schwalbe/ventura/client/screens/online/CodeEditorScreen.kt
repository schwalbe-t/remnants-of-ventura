
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.net.PacketHandler
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.Vector4fc
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

private val CODE_EDITING_SETTINGS = CodeEditingSettings(
    paired = listOf("()", "[]", "{}", "\"\""),
    autoIndent = true
)

private const val EDITOR_SAVE_DELAY_MS: Long = 250

private class Editor(val file: File, fileContent: String) {

    val lineNumbers: Text
    val lineNumberCont: Padding
    val textInput: TextInput
    val textScroll: Scroll
    val root: UiElement

    val path: Path = this.file.toPath()
    var lastEdit: Long? = null

    init {
        val fontSize: UiSize = 1.5.vmin
        this.lineNumbers = Text()
            .withFont(jetbrainsMonoSb())
            .withColor(SECONDARY_BRIGHT_FONT_COLOR)
            .withSize(fontSize)
            .alignRight()
        this.lineNumberCont = Padding()
            .withPadding(0.px)
            .withContent(this.lineNumbers)
        this.textInput = TextInput()
            .withContent(Text()
                .withFont(jetbrainsMonoSb())
                .withColor(BRIGHT_FONT_COLOR)
                .withSize(fontSize)
            )
            .withDisplayedSpans(::syntaxHighlightBigton)
            .withMultilineInput(multiline = true)
            .withValue(fileContent)
            .withCodeTypedHandler(CODE_EDITING_SETTINGS)
            .withCodeDeletedHandler(CODE_EDITING_SETTINGS)
            .withValueChangedHandler {
                this.updateLineNumbers()
                this.lastEdit = System.currentTimeMillis()
            }
        this.textScroll = this.textInput
            .wrapScrolling(horiz = true, vert = true)
            .withThumbColor(BUTTON_COLOR)
            .withThumbHoverColor(BUTTON_HOVER_COLOR)
        this.root = Axis.column()
            .add(5.vmin, Text()
                .withText(this.path.name)
                .withFont(googleSansSb())
                .withColor(BRIGHT_FONT_COLOR)
                .withSize(85.ph)
                .pad(1.5.vmin)
            )
            .add(100.ph - 5.vmin, Axis.row()
                .add(5.vmin, this.lineNumberCont)
                .add(3.vmin, Space())
                .add(100.pw - 5.vmin - 3.vmin, this.textScroll)
                .pad(0.5.vmin)
            )
        this.updateLineNumbers()
    }

    fun updateLineNumbers() {
        val lineNumberText = (1..this.textInput.computeLineCount())
            .joinToString(transform = Int::toString, separator = "\n")
        this.lineNumbers.withText(lineNumberText)
    }

    var lastScrollY: Float = 0f

    fun update() {
        val newScrollY: Float = -this.textScroll.scrollOffset.y()
        if (newScrollY != this.lastScrollY) {
            this.lineNumberCont.withPadding(
                top = newScrollY.px,
                bottom = 0.px, left = 0.px, right = 0.px
            )
            this.lineNumbers.invalidate()
            this.lastScrollY = newScrollY
        }
        val lastEdit: Long? = this.lastEdit
        val now: Long = System.currentTimeMillis()
        if (lastEdit != null && lastEdit + EDITOR_SAVE_DELAY_MS <= now) {
            this.save()
        }
    }

    fun save() {
        if (this.lastEdit == null) { return }
        Files.writeString(this.path, this.textInput.valueString)
        this.lastEdit = null
    }

}

private val FILE_TREE_BACKGROUND: Vector4fc
        = Vector4f(0.2f, 0.2f, 0.2f, 0.35f)
private val CODE_EDITOR_BACKGROUND: Vector4fc
    = Vector4f(0.2f, 0.2f, 0.2f, 0.90f)

fun codeEditingScreen(client: Client): () -> GameScreen = {
    val background = BlurBackground()
        .withRadius(3)
        .withSpread(5)
    val contextMenu = ContextMenu()
    val editorCont = Stack()
    var editor: Editor? = null
    fun openFile(file: File) {
        val content: String
        try {
            content = Files.readString(file.toPath())
        } catch (e: Exception) {
            return e.printStackTrace()
        }
        val newEditor = Editor(file, content)
        editorCont.disposeAll()
        editorCont.add(newEditor.root)
        editor = newEditor
    }
    fun closeFile() {
        editor?.save()
        editorCont.disposeAll()
        editor = null
    }
    val screen = GameScreen(
        onOpen = {
            client.world?.let {
                it.camController.mode = CameraModes.playerFarCentered(it.player)
            }
        },
        render = {
            if (Key.ESCAPE.wasPressed || Key.C.wasPressed) {
                client.nav.pop()
            }
            SourceFiles.update(client)
            client.world?.update(client, captureInput = false)
            client.world?.player?.assertAnimation(PlayerAnim.thinking)
            client.world?.render(client)
            background.invalidate()
            editor?.update()
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
    screen.add(layer = 0, element = Stack()
        .add(background)
        .add(FlatBackground().withColor(FILE_TREE_BACKGROUND))
        .add(Axis.row()
            .add(1f/3f * fpw, Stack()
                .add(FlatBackground().withColor(FILE_TREE_BACKGROUND))
                .add(createFileTree(FileTreeCtx(
                    contextMenu,
                    onFileSelect = { openFile(it) },
                    isMutable = true,
                    onFileDelete = { file ->
                        if (editor?.file?.canonicalPath == file.canonicalPath) {
                            closeFile()
                        }
                    },
                    onFileRename = { from, to ->
                        if (editor?.file?.canonicalPath == from.canonicalPath) {
                            closeFile()
                            openFile(to)
                        }
                    }
                )))
            )
            .add(2f/3f * fpw, Stack()
                .add(FlatBackground().withColor(CODE_EDITOR_BACKGROUND))
                .add(editorCont)
            )
        )
    )
    screen.add(layer = 1, element = contextMenu)
    screen
}
