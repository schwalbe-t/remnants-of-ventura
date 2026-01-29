
package schwalbe.ventura.server

import schwalbe.ventura.net.Packet
import schwalbe.ventura.net.PacketType
import kotlinx.serialization.Serializable
import schwalbe.ventura.net.SerVector3

@Serializable
data class PlayerData(
    val worlds: MutableList<WorldEntry>
) {
    
    companion object {}

    @Serializable
    data class WorldEntry(
        val worldId: Long,
        var position: SerVector3
    )

}

class Player(
    val username: String,
    val data: PlayerData,
    val connection: Server.Connection
) {}

fun Player.getCurrentWorld(worlds: WorldRegistry): World {
    var world: World? = null
    while (this.data.worlds.isNotEmpty()) {
        val worldId: Long = this.data.worlds.last().worldId
        val found: World? = worlds[worldId]
        if (found != null) {
            world = found
            break
        }
        this.data.worlds.removeLast()
    }
    if (world == null) {
        world = worlds.baseWorld
        this.data.worlds.add(worlds.baseWorld.createPlayerEntry())
    }
    return world
}

fun Player.pushWorld(newWorldId: Long, worlds: WorldRegistry): Boolean {
    val newWorld: World = worlds[newWorldId]
        ?: return false
    val currentWorldId: Long? = this.data.worlds.lastOrNull()?.worldId
    if (currentWorldId != null) {
        val currentWorld: World? = worlds[currentWorldId]
        currentWorld?.handlePlayerLeaving(this)
    }
    this.connection.outgoing.send(Packet.serialize(
        PacketType.DOWN_BEGIN_WORLD_CHANGE, Unit
    ))
    newWorld.transfer(this)
    return true
}

fun Player.popWorld(worlds: WorldRegistry) {
    val leftWorldId: Long? = this.data.worlds.removeLastOrNull()?.worldId
    if (leftWorldId != null) {
        val leftWorld: World? = worlds[leftWorldId]
        leftWorld?.handlePlayerLeaving(this)
    }
    val current: World = this.getCurrentWorld(worlds)
    this.connection.outgoing.send(Packet.serialize(
        PacketType.DOWN_BEGIN_WORLD_CHANGE, Unit
    ))
    current.transfer(this)
}