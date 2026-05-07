
package schwalbe.ventura.server.game

import kotlinx.serialization.Serializable
import schwalbe.ventura.PaletteColor
import schwalbe.ventura.PaletteColor.*
import schwalbe.ventura.net.SharedPlayerInfo
import schwalbe.ventura.data.*
import kotlin.uuid.Uuid

private fun generatePlayerStyle(): PersonStyle {
    val isMasculine: Boolean = arrayOf(true, false).random()
    val hairColor: PaletteColor = arrayOf(BLACK, DARK_BROWN, BROWN).random()
    val hoodieColor: PaletteColor = arrayOf(
        DARK_GRAY, GRAY, BRIGHT_GRAY, BLUE, PURPLE, MAGENTA, PINK,
        YELLOW, BRIGHT_ORANGE, ORANGE, RED, DARK_RED, DARK_BLUE,
        DARK_GREEN, CYAN, GREEN, AQUA
    ).random()
    val pantsColor: PaletteColor = arrayOf(DARK_BROWN, WHITE).random()
    val legsColor: PaletteColor = when {
        isMasculine -> CREAM
        else -> BLACK
    }
    val irisColor: PaletteColor = arrayOf(
        DARK_BROWN, BROWN, DARK_GRAY, PURPLE, WINE, BLUE, GREEN
    ).random()
    return PersonStyle(
        colors = PersonColorType.makeColors(
            PersonColorType.SKIN to CREAM,
            PersonColorType.HAIR to hairColor,
            PersonColorType.EYEBROWS to hairColor,
            PersonColorType.HOODIE to hoodieColor,
            PersonColorType.PANTS to pantsColor,
            PersonColorType.LEGS to legsColor,
            PersonColorType.SHOES to WHITE,
            PersonColorType.HANDS to CREAM,
            PersonColorType.IRIS to irisColor,
            PersonColorType.EYES to WHITE
        ),
        hair = when {
            isMasculine -> PersonHairStyle.SHORT
            else -> PersonHairStyle.LONG
        }
    )
}

@Serializable
data class PlayerData(
    val worlds: MutableList<WorldEntry> = mutableListOf(),
    val inventory: Inventory = Inventory(),
    var purse: Purse = Purse(),
    val deployedRobots: MutableMap<Uuid, PlayerRobot> = mutableMapOf(),
    val sourceFiles: SourceFiles = SourceFiles(),
    var style: PersonStyle = generatePlayerStyle()
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
    val style = generatePlayerStyle()
    return PlayerData(
        worlds = mutableListOf(worlds.baseWorld.createPlayerEntry(style)),
        inventory = Inventory(itemCounts = mutableMapOf(
            Item(ItemType.KENDAL_DYNAMICS_SCOUT, ItemVariant.SCOUT_CAMOUFLAGE) to 1,
            Item(ItemType.BIGTON_1030, null) to 1
        )),
        purse = Purse(numCoins = 0),
        style = style
    )
}
