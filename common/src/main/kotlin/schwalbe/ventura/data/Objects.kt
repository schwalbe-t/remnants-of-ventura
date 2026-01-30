
package schwalbe.ventura.data

import kotlinx.serialization.Serializable
import schwalbe.ventura.net.SerVector3

@Serializable
enum class ObjectType(
    val modelPath: String,
    val renderOutline: Boolean = true,
    val applyColliders: Boolean = true
) {
    ROCK(
        modelPath = "res/objects/rock.glb"
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