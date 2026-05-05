
package schwalbe.ventura.server.game

import schwalbe.ventura.net.Packet
import schwalbe.ventura.net.PacketType
import schwalbe.ventura.server.Server
import kotlin.uuid.Uuid

class Player(
    val username: String,
    val data: PlayerData,
    val connection: Server.Connection
) {
    var lastChatMessage: Long = 0L
}

fun Player.getCurrentWorld(worlds: WorldRegistry): World {
    var world: World? = null
    while (this.data.worlds.isNotEmpty()) {
        val worldId: Uuid = this.data.worlds.last().worldId
        val found: World? = worlds[worldId]
        if (found != null) {
            world = found
            break
        }
        this.data.worlds.removeLast()
    }
    if (world == null) {
        world = worlds.baseWorld
        this.data.worlds.add(
            worlds.baseWorld.createPlayerEntry(this.data.style)
        )
    }
    return world
}

fun Player.pushWorld(newWorldId: Uuid, worlds: WorldRegistry): Boolean {
    val newWorld: World = worlds[newWorldId]
        ?: return false
    val currentWorldId: Uuid? = this.data.worlds.lastOrNull()?.worldId
    if (currentWorldId != null) {
        val currentWorld: World? = worlds[currentWorldId]
        currentWorld?.handlePlayerLeaving(this)
    }
    this.data.worlds.add(newWorld.createPlayerEntry(this.data.style))
    this.connection.outgoing.send(Packet.serialize(
        PacketType.BEGIN_WORLD_CHANGE, Unit
    ))
    newWorld.transfer(this)
    return true
}

fun Player.popWorld(worlds: WorldRegistry) {
    if (this.data.worlds.size <= 1) { return }
    val leftWorldId: Uuid = this.data.worlds.removeLast().worldId
    val leftWorld: World? = worlds[leftWorldId]
    leftWorld?.handlePlayerLeaving(this)
    val current: World = this.getCurrentWorld(worlds)
    this.connection.outgoing.send(Packet.serialize(
        PacketType.BEGIN_WORLD_CHANGE, Unit
    ))
    current.transfer(this)
}

fun Player.updateState(world: World) {
    for (robot in this.data.deployedRobots.values.filter { it.health <= 0f }) {
        for (item in robot.collectContainedItems()) {
            if (Math.random() > PlayerRobot.DESTRUCT_DROP_CHANCE) { continue }
            world.spawnItem(robot.position, item, ownerName = this.username)
        }
        this.data.deployedRobots.remove(robot.id)
    }
    this.data.deployedRobots.values.forEach {
        it.update(world, this)
    }
    this.data.sourceFiles.removeUnused { markFileUsed ->
        this.data.deployedRobots.values.forEach {
            it.sourceFiles.forEach(markFileUsed)
        }
    }
}