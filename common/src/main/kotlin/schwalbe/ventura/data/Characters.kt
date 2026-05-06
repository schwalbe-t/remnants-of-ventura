
package schwalbe.ventura.data

import kotlinx.serialization.Serializable
import schwalbe.ventura.PaletteColor.*

@Serializable
enum class CharacterStylePreset(val style: PersonStyle) {
    MERCHANT(PersonStyle(
        colors = PersonColorType.makeColors(
            PersonColorType.SKIN to CREAM,
            PersonColorType.HAIR to GREEN,
            PersonColorType.EYEBROWS to GREEN,
            PersonColorType.HOODIE to GREEN,
            PersonColorType.PANTS to GREEN,
            PersonColorType.LEGS to BLACK,
            PersonColorType.SHOES to GREEN,
            PersonColorType.HANDS to BLACK,
            PersonColorType.IRIS to BROWN,
            PersonColorType.EYES to WHITE
        ),
        hair = PersonHairStyle.SHORT
    ))
}
