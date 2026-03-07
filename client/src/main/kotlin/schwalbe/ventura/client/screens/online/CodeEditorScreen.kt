
package schwalbe.ventura.client.screens.online

import org.joml.Vector3f
import schwalbe.ventura.client.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.net.PacketHandler

private val PLACER_CENTERED = CameraController.Mode(
    lookAt = { _, w, _ -> Vector3f()
        .add(w.player.position)
        .add(0f, +1.25f, 0f)
    },
    fovDegrees = 20f,
    distance = { _ -> 10f }
)

private val CODE_EDITING_SETTINGS = CodeEditingSettings(
    paired = listOf("()", "[]", "{}", "\"\""),
    autoIndent = true
)

fun codeEditingScreen(client: Client): () -> GameScreen = {
    val background = BlurBackground()
        .withRadius(3)
        .withSpread(5)
    val fontSize: UiSize = 1.5.vmin
    val lineNumbers = Text()
        .withFont(jetbrainsMonoSb())
        .withColor(SECONDARY_BRIGHT_FONT_COLOR)
        .withSize(fontSize)
        .alignRight()
    val lineNumberCont = Padding()
        .withPadding(0.px)
        .withContent(lineNumbers)
    val editorTextInput = TextInput()
        .withContent(Text()
            .withFont(jetbrainsMonoSb())
            .withColor(BRIGHT_FONT_COLOR)
            .withSize(fontSize)
        )
        .withMultilineInput(multiline = true)
        .withCodeTypedHandler(CODE_EDITING_SETTINGS)
        .withCodeDeletedHandler(CODE_EDITING_SETTINGS)
    val editorTextScroll = editorTextInput
        .wrapScrolling(horiz = true, vert = true)
        .withThumbColor(BUTTON_COLOR)
        .withThumbHoverColor(BUTTON_HOVER_COLOR)
    fun updateLineNumbers() {
        val lineNumberText = (1..editorTextInput.computeLineCount())
            .joinToString(transform = Int::toString, separator = "\n")
        lineNumbers.withText(lineNumberText)
    }
    editorTextInput.onValueChanged = { updateLineNumbers() }
    updateLineNumbers()
    var lastScrollY = 0f
    fun updateEditor() {
        val newScrollY: Float = -editorTextScroll.scrollOffset.y()
        if (newScrollY != lastScrollY) {
            lineNumberCont.withPadding(
                top = newScrollY.px,
                bottom = 0.px, left = 0.px, right = 0.px
            )
            lineNumbers.invalidate()
            lastScrollY = newScrollY
        }
    }
    val screen = GameScreen(
        onOpen = {
            client.world?.camController?.mode = PLACER_CENTERED
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
            updateEditor()
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
        .add(FlatBackground().withColor(PANEL_BACKGROUND))
        .add(Axis.row()
            .add(1f/3f * fpw, Text().withText("File Tree"))
            .add(2f/3f * fpw, Stack()
                .add(FlatBackground().withColor(PANEL_BACKGROUND))
                .add(Axis.column()
                    .add(5.vmin, Text()
                        .withText("File Name")
                        .withFont(jetbrainsMonoB())
                        .withColor(BRIGHT_FONT_COLOR)
                        .withSize(75.ph)
                        .pad(1.5.vmin)
                    )
                    .add(100.ph - 5.vmin, Axis.row()
                        .add(5.vmin, lineNumberCont)
                        .add(3.vmin, Space())
                        .add(100.pw - 5.vmin - 3.vmin, editorTextScroll)
                        .pad(0.5.vmin)
                    )
                )
            )
        )
    )
    screen
}
