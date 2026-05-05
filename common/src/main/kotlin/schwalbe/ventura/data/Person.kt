
package schwalbe.ventura.data

import kotlinx.serialization.Serializable
import schwalbe.ventura.PaletteColor
import schwalbe.ventura.utils.SerVector3

@Serializable
enum class SharedPersonAnimation {
    IDLE, WALK, THINKING, SQUAT
}

@Serializable
enum class PersonColorType(val phR: Float, val phG: Float, val phB: Float) {
    SKIN    (1.0f, 0.5f, 0.0f),
    HAIR    (0.0f, 0.5f, 0.0f),
    EYEBROWS(0.5f, 0.5f, 0.0f),
    HOODIE  (1.0f, 0.0f, 1.0f),
    PANTS   (0.5f, 0.0f, 1.0f),
    LEGS    (0.0f, 0.0f, 1.0f),
    SHOES   (1.0f, 0.0f, 0.5f),
    HANDS   (1.0f, 0.5f, 0.5f),
    IRIS    (0.5f, 0.0f, 0.0f),
    EYES    (1.0f, 1.0f, 1.0f);

    val localNameKey: String
        get() = "PersonColorType:name/${this.name}"
}

fun PersonColorType.Companion.makeColors(
    vararg mappings: Pair<PersonColorType, PaletteColor>
): List<SerVector3> {
    val result = Array(PersonColorType.entries.size) { SerVector3(0f, 0f, 0f) }
    for ((type, color) in mappings) {
        result[type.ordinal] = color.ser
    }
    return result.toList()
}

@Serializable
enum class PersonHairStyle {
    LONG, SHORT;

    val localNameKey: String
        get() = "PersonHairStyle:name/${this.name}"
}

@Serializable
data class PersonStyle(
    val colors: List<SerVector3>,
    val hair: PersonHairStyle
)
