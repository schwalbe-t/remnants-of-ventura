
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.Client
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.clearError
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.GameScreen
import schwalbe.ventura.client.screens.Theme
import schwalbe.ventura.client.screens.googleSansSb
import schwalbe.ventura.client.screens.keepNetworkConnectionAlive
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.data.ObjectInstance
import schwalbe.ventura.data.ObjectProp
import schwalbe.ventura.data.RemoteLocalization
import schwalbe.ventura.engine.input.Key
import schwalbe.ventura.engine.input.MButton
import schwalbe.ventura.engine.input.wasPressed
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.net.PacketHandler
import schwalbe.ventura.net.PacketType
import schwalbe.ventura.utils.toVector3f

fun characterDialogueScreen(
    client: Client, character: ObjectInstance
): () -> GameScreen = {
    val nextSections: MutableList<RemoteLocalization.Dialogue> = mutableListOf()
    val nextLines: MutableList<String> = mutableListOf()
    val currentName = Text()
    val currentLine = Text()
    fun onDialogueComplete() {
        client.nav.pop()
    }
    fun onShowNextSection(): Boolean {
        val section: RemoteLocalization.Dialogue?
            = nextSections.removeFirstOrNull()
        if (section == null) {
            onDialogueComplete()
            return false
        }
        currentName.withText(section.name)
        nextLines.clear()
        nextLines.addAll(section.lines)
        return true
    }
    fun onShowNextLine() {
        val line: String? = nextLines.removeFirstOrNull()
        if (line == null) {
            if (onShowNextSection()) { onShowNextLine() }
            return
        }
        currentLine.withText(line)
    }
    fun onDialogueReceived(dialogue: List<RemoteLocalization.Dialogue>) {
        nextSections.clear()
        nextSections.addAll(dialogue)
        nextLines.clear()
        onShowNextLine()
    }
    val background = BlurBackground()
        .withRadius(3)
        .withSpread(5)
    val keybinds = KeybindDisplay(listOf(
        Keybind(KEYBIND_SKIP_DIALOGUE, listOf(Keybind.Key("Esc", 2f))),
        Keybind(KEYBIND_NEXT_DIALOGUE, listOf(
            Keybind.Key("Q"), Keybind.Key("␣"), Keybind.Key("LMB", 2f)
        ))
    ))
    val screen = GameScreen(
        onOpen = {
            client.world?.let {
                it.camController.mode = CameraModes.characterCentered(character)
            }
        },
        render = render@{
            if (Key.ESCAPE.wasPressed) {
                client.nav.pop()
            }
            val showNextLine: Boolean = Key.Q.wasPressed
                || Key.SPACE.wasPressed || MButton.LEFT.wasPressed
            if (showNextLine) {
                onShowNextLine()
            }
            SourceFiles.update(client)
            val world: World = client.world ?: return@render
            world.update(client, captureInput = false)
            val characterPos = character[ObjectProp.Position].toVector3f()
                .add(0f, +1.5f, 0f)
            world.player.facePoint(characterPos)
            world.player.assertAnimation(PersonAnim.idle)
            world.render(client)
            background.invalidate()
            keybinds.update(client.config.settings.showControls)
        },
        networkState = keepNetworkConnectionAlive(client, onFail = { reason ->
            client.nav.replace(serverConnectionFailedScreen(reason, client))
            client.network.clearError()
        }),
        packets = PacketHandler.receiveDownPackets<Unit>()
            .addErrorLogging()
            .addWorldHandling(client)
            .updateStoredSources(client)
            .addChatMessageHandling(client)
            .onPacket(PacketType.RECEIVE_DIALOGUE) { dialogue, _ ->
                onDialogueReceived(dialogue)
            },
        navigator = client.nav
    )
    screen.add(layer = 0, element = Axis.column()
        .add(3f/4f * fph, Space())
        .add(1f/4f * fph, Stack()
            .add(background)
            .add(FlatBackground().withColor(Theme.PANEL_BACKGROUND))
            .add(Axis.column()
                .add(5.vmin, currentName
                    .withFont(googleSansSb())
                    .withSize(2.5.vmin)
                )
                .add(100.ph - 5.vmin, currentLine
                    .withSize(1.75.vmin)
                )
                .pad(horizontal = 25.pw, vertical = 1.5.vmin)
            )
        )
    )
    screen.add(layer = 1, element = keybinds.createRootMount())
    screen
}
