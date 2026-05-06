
package schwalbe.ventura.data

import kotlinx.serialization.Serializable
import schwalbe.ventura.PaletteColor.*

@Serializable
enum class CharacterStylePreset(val style: PersonStyle) {
    MERCHANT(PersonStyle(
        colors = PersonColorType.makeColors(
            PersonColorType.SKIN to CREAM,
            PersonColorType.HAIR to WINE,
            PersonColorType.EYEBROWS to DARK_BROWN,
            PersonColorType.HOODIE to DARK_GREEN,
            PersonColorType.PANTS to DARK_GREEN,
            PersonColorType.LEGS to BLACK,
            PersonColorType.SHOES to DARK_GREEN,
            PersonColorType.HANDS to BLACK,
            PersonColorType.IRIS to BROWN,
            PersonColorType.EYES to WHITE
        ),
        hair = PersonHairStyle.SHORT
    ))
}
