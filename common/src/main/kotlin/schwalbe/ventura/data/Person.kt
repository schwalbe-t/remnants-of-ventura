
package schwalbe.ventura.data

import kotlinx.serialization.Serializable
import schwalbe.ventura.utils.SerVector3

@Serializable
enum class SharedPersonAnimation {
    IDLE, WALK, THINKING, SQUAT
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
