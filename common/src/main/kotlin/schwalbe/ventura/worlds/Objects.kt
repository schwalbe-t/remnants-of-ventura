
package schwalbe.ventura.worlds

import kotlinx.serialization.Serializable
import org.joml.Vector3fc

@Serializable
enum class ObjectType(
    val filePath: String
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
    val position: Vector3fc,
    val rotation: Vector3fc,
    val scale: Vector3fc,
    val param: ObjectParam = ObjectParam.None
)