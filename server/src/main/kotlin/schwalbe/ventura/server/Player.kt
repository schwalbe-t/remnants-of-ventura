
package schwalbe.ventura.server

import schwalbe.ventura.data.Item
import schwalbe.ventura.data.ItemType
import schwalbe.ventura.net.Packet
import schwalbe.ventura.net.PacketType
import schwalbe.ventura.net.SharedPlayerInfo
import kotlinx.serialization.Serializable

@Serializable
data class PlayerData(
    val worlds: MutableList<WorldEntry>,
    val inventoryItemCounts: MutableMap<Item, Int>
) {
    
    companion object;

    @Serializable
    data class WorldEntry(
        val worldId: Long,
        var state: SharedPlayerInfo
    )

}

fun PlayerData.Companion.createStartingData(
    worlds: WorldRegistry
) = PlayerData(
    worlds = mutableListOf(worlds.baseWorld.createPlayerEntry()),
    inventoryItemCounts = mutableMapOf(
        Item(ItemType.KENDAL_DYNAMICS_SCOUT, null) to 99999,
        Item(ItemType.BIGTON_1030, null) to 99999,
        Item(ItemType.BIGTON_1050, null) to 99999,
        Item(ItemType.BIGTON_1070, null) to 99999,
        Item(ItemType.BIGTON_2030, null) to 99999,
        Item(ItemType.BIGTON_2050, null) to 99999,
        Item(ItemType.BIGTON_2070, null) to 99999,
        Item(ItemType.BIGTON_3030, null) to 99999,
        Item(ItemType.BIGTON_3050, null) to 99999,
        Item(ItemType.BIGTON_3070, null) to 99999,
        Item(ItemType.PIVOTAL_ME2048, null) to 99999,
        Item(ItemType.PIVOTAL_ME5120, null) to 99999,
        Item(ItemType.PIVOTAL_ME10K, null) to 99999,
        Item(ItemType.PIVOTAL_ME20K, null) to 99999
    )
)


class Player(
    val username: String,
    val data: PlayerData,
    val connection: Server.Connection
)

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
        PacketType.BEGIN_WORLD_CHANGE, Unit
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
        PacketType.BEGIN_WORLD_CHANGE, Unit
    ))
    current.transfer(this)
}