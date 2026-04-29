
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.net.PacketHandler
import schwalbe.ventura.net.WorldStatePacket
import schwalbe.ventura.utils.toVector3f
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

fun controllingPlayerScreen(client: Client): () -> GameScreen = {
    val playerNames = NameDisplayManager()
    val robotStatus = RobotStatusDisplayManager()
    fun advancedEditing(): Boolean
        = client.config.settings.advancedEditingEnabled
    val screen = GameScreen(
        onOpen = {
            client.nav.retainCurrent()
            client.world?.let {
                it.camController.mode = it.playerAtCenterCamMode
            }
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
            client.world?.update(client, captureInput = true)
            client.world?.render(client)
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
    screen.add(layer = 0, element = playerNames.container)
    screen.add(layer = 1, element = robotStatus.container)
    screen
}

