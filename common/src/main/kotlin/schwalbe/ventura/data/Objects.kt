
package schwalbe.ventura.data

import kotlinx.serialization.Serializable
import schwalbe.ventura.net.SerVector3

// The maximum size in chunks of any client or server side colliders.
// This number determines the radius of the area that is searched when looking
// for colliders that intersect a given point, both on the client and the
// server. Models that are larger than this constant may sometimes not be
// computed properly by client and server collision systems.
const val MAX_COLLIDER_SIZE_CHUNKS: Int = 1

@Serializable
enum class ObjectType(
    val modelPath: String,
    val renderOutline: Boolean = true,
    val applyColliders: Boolean = true,
    val tileColliderRadius: Int
) {
    ROCK(
        modelPath = "res/objects/rock.glb",
        tileColliderRadius = 3
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