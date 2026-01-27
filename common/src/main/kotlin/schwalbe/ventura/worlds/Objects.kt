
package schwalbe.ventura.worlds

import kotlinx.serialization.Serializable
import schwalbe.ventura.net.SerVector3

@Serializable
enum class ObjectType(
    val filePath: String, val renderOutline: Boolean = true
) {
    ROCK(
        filePath = "res/objects/rock.glb"
    )
}

@Serializable
sealed interface ObjectParam {
    object None : ObjectParam
}

@Serializable
data class ObjectInstance(
    val type: ObjectType,
    val position: SerVector3,
    val rotation: SerVector3,
    val scale: SerVector3,
    val param: ObjectParam = ObjectParam.None
)