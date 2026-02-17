
package schwalbe.ventura.server.game

import kotlinx.serialization.Serializable
import schwalbe.ventura.data.Item
import schwalbe.ventura.data.ItemType
import schwalbe.ventura.net.SharedPlayerInfo
import kotlin.uuid.Uuid

@Serializable
data class PlayerData(
    val worlds: MutableList<WorldEntry> = mutableListOf(),
    val inventory: Inventory = Inventory(),
    val deployedRobots: MutableMap<Uuid, Robot> = mutableMapOf(),
    val sourceFiles: SourceFiles = SourceFiles()
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
    inventory = Inventory(itemCounts = mutableMapOf(
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
    ))
)
