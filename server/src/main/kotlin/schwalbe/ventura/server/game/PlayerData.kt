
package schwalbe.ventura.server.game

import kotlinx.serialization.Serializable
import schwalbe.ventura.data.Item
import schwalbe.ventura.data.ItemType
import schwalbe.ventura.data.ItemVariant
import schwalbe.ventura.net.SharedPlayerInfo
import schwalbe.ventura.utils.SerVector3
import schwalbe.ventura.utils.parseRgbHex
import schwalbe.ventura.utils.toSerVector3
import kotlin.uuid.Uuid

private val PLAYER_HOODIE_COLORS: List<SerVector3> = listOf(
    parseRgbHex("5a8b97").toSerVector3(),
    parseRgbHex("cc785b").toSerVector3(),
    parseRgbHex("ba5e69").toSerVector3(),
    parseRgbHex("aa749e").toSerVector3(),
    parseRgbHex("437f5d").toSerVector3()
)

private val PLAYER_IRIS_COLORS: List<SerVector3> = listOf(
    parseRgbHex("443331").toSerVector3(),
    parseRgbHex("525979").toSerVector3(),
    parseRgbHex("437f5d").toSerVector3()
)

private fun generatePlayerColors(): List<SerVector3> = listOf(
    parseRgbHex("d4a488").toSerVector3(),
    parseRgbHex("443331").toSerVector3(),
    parseRgbHex("50473f").toSerVector3(),
    PLAYER_HOODIE_COLORS.random(),
    parseRgbHex("50473f").toSerVector3(),
    parseRgbHex("443331").toSerVector3(),
    parseRgbHex("d1c19e").toSerVector3(),
    parseRgbHex("d4a488").toSerVector3(),
    PLAYER_IRIS_COLORS.random(),
    parseRgbHex("eae2ce").toSerVector3()
)

@Serializable
data class PlayerData(
    val worlds: MutableList<WorldEntry> = mutableListOf(),
    val inventory: Inventory = Inventory(),
    val deployedRobots: MutableMap<Uuid, PlayerRobot> = mutableMapOf(),
    val sourceFiles: SourceFiles = SourceFiles(),
    var colors: List<SerVector3> = generatePlayerColors()
) {

    companion object {
        const val MAX_NUM_WORLDS: Int = 64
    }

    @Serializable
    data class WorldEntry(
        val worldId: Uuid,
        var state: SharedPlayerInfo
    )

}

fun PlayerData.Companion.createStartingData(
    worlds: WorldRegistry
): PlayerData {
    val colors = generatePlayerColors()
    return PlayerData(
        worlds = mutableListOf(worlds.baseWorld.createPlayerEntry(colors)),
        inventory = Inventory(itemCounts = mutableMapOf(
            Item(ItemType.KENDAL_DYNAMICS_SCOUT, ItemVariant.SCOUT_CAMOUFLAGE) to 10,
            Item(ItemType.KENDAL_DYNAMICS_SCOUT, ItemVariant.SCOUT_FIREWORKS) to 10,
            Item(ItemType.KENDAL_DYNAMICS_SCOUT, ItemVariant.SCOUT_LADYBUG) to 10,
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
            Item(ItemType.PIVOTAL_ME20K, null) to 99999,
            Item(ItemType.DIGITAL_RADIO, null) to 99999,
            Item(ItemType.GPS_RECEIVER, null) to 99999,
            Item(ItemType.SHORT_RANGE_SONAR, null) to 99999,
            Item(ItemType.MID_RANGE_SONAR, null) to 99999,
            Item(ItemType.LONG_RANGE_SONAR, null) to 99999,
            Item(ItemType.SHORT_RANGE_RADAR, null) to 99999,
            Item(ItemType.MID_RANGE_RADAR, null) to 99999,
            Item(ItemType.LONG_RANGE_RADAR, null) to 99999,
            Item(ItemType.LASER, null) to 99999
        )),
        colors = colors
    )
}
