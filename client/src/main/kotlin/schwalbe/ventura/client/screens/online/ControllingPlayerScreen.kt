
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.net.PacketHandler
import schwalbe.ventura.net.WorldStatePacket
import schwalbe.ventura.utils.toVector3f
import schwalbe.ventura.engine.ui.*
import org.joml.Vector3fc
import kotlin.uuid.Uuid

private fun findClosestOwnedRobot(
    worldState: WorldStatePacket, observer: Vector3fc
): Pair<Uuid?, Float> {
    var closestId: Uuid? = null
    var closestDist: Float = Float.POSITIVE_INFINITY
    for (id in worldState.ownedRobots.keys) {
        val state = worldState.allRobots[id] ?: continue
        val dist = state.position.toVector3f().distance(observer)
        if (dist >= closestDist) { continue }
        closestId = id
        closestDist = dist
    }
    return closestId to closestDist
}

const val MAX_ROBOT_EDIT_DIST: Float = 2f
val KEYBINDS_WIDTH: UiSize = 20.vw

private fun hasRobotInRange(client: Client): Boolean {
    val world = client.world ?: return false
    val worldState = world.state.lastReceived ?: return false
    val (closestId, closestDist)
        = findClosestOwnedRobot(worldState, world.player.position)
    return closestId != null && closestDist <= MAX_ROBOT_EDIT_DIST
}

private fun defineKeybinds(client: Client): List<Keybind> = listOf(
    Keybind(
        KEYBIND_MOVE, listOf(
            Keybind.Key("W"), Keybind.Key("A"),
            Keybind.Key("S"), Keybind.Key("D")
        )
    ),
    Keybind(KEYBIND_MENU, listOf(Keybind.Key("Esc", 2f))),
    Keybind(KEYBIND_INVENTORY, listOf(Keybind.Key("Tab", 2f))),
    Keybind(
        KEYBIND_EDIT_ADVANCED_CODE, listOf(Keybind.Key("C")),
        displayIf = { client.config.settings.advancedEditingEnabled }
    ),
    Keybind(
        KEYBIND_EDIT_ROBOT_CODE, listOf(Keybind.Key("C")),
        displayIf = {
            !client.config.settings.advancedEditingEnabled
                && hasRobotInRange(client)
        }
    ),
    Keybind(
        KEYBIND_CONFIGURE_ROBOT, listOf(Keybind.Key("E")),
        displayIf = { hasRobotInRange(client) }
    )
)

fun controllingPlayerScreen(client: Client): () -> GameScreen = {
    val playerNames = NameDisplayManager()
    val robotStatus = RobotStatusDisplayManager()
    val chat = ChatBox(client)
    val keybinds = KeybindDisplay(defineKeybinds(client))
    fun advancedEditing(): Boolean
        = client.config.settings.advancedEditingEnabled
    val screen = GameScreen(
        onOpen = {
            client.nav.retainCurrent()
            client.world?.let {
                it.camController.mode = it.playerAtCenterCamMode
            }
            chat.renderMessageLog()
        },
        render = render@{
            val world: World = client.world ?: return@render
            if (Key.ESCAPE.wasPressed) {
                client.nav.push(escapeMenuScreen(client))
            }
            if (Key.TAB.wasPressed) {
                client.nav.push(inventoryMenuScreen(client))
            }
            if (Key.C.wasPressed && advancedEditing()) {
                client.nav.push(multiFileEditorScreen(client))
            }
            SourceFiles.update(client)
            val worldState: WorldStatePacket? = world.state.lastReceived
            if (worldState != null) {
                val (closestId, closestDist)
                    = findClosestOwnedRobot(worldState, world.player.position)
                val hasInRange: Boolean = closestId != null
                    && closestDist <= MAX_ROBOT_EDIT_DIST
                if (hasInRange && Key.E.wasPressed) {
                    client.nav.push(robotEditingScreen(client, closestId))
                }
                if (hasInRange && Key.C.wasPressed && !advancedEditing()) {
                    client.nav.push(robotFileEditorScreen(client, closestId))
                }
                robotStatus.update(client, worldState)
            }
            client.world?.state?.activeNameDisplays = playerNames
            client.world?.update(client, captureInput = !chat.isTyping)
            if (chat.isTyping) {
                client.world?.player?.assertAnimation(PersonAnim.thinking)
            }
            client.world?.render(client)
            if (client.config.settings.showChat) {
                chat.root.withSize(30.vw, 30.vh)
            } else {
                chat.root.withSize(0.px, 0.px)
            }
            chat.update()
            if (client.config.settings.showControls) {
                keybinds.root.withSize(KEYBINDS_WIDTH, 0.px)
            } else {
                keybinds.root.withSize(0.px, 0.px)
            }
            keybinds.update()
        },
        networkState = keepNetworkConnectionAlive(client, onFail = { reason ->
            client.nav.replace(serverConnectionFailedScreen(reason, client))
            client.network.clearError()
        }),
        packets = PacketHandler.receiveDownPackets<Unit>()
            .addErrorLogging()
            .addWorldHandling(client)
            .updateStoredSources(client)
            .addChatMessageHandling(client),
        navigator = client.nav
    )
    chat.handleChatMessages(screen.packets!!)
    screen.add(layer = 0, element = playerNames.container)
    screen.add(layer = 1, element = robotStatus.container)
    screen.add(layer = 2, element = chat.root
        .pad(2.5.vmin)
    )
    screen.add(layer = 3, element = keybinds.root
        .pad(left = 100.pw - KEYBINDS_WIDTH - 2.5.vmin, top = 2.5.vmin)
    )
    screen
}

