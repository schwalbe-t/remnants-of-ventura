
package schwalbe.ventura.data

import kotlinx.serialization.SerialName
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
    ),

    TREE(
        modelPath = "res/objects/rock.glb",
        tileColliderRadius = 3
    ),
    GRASS(
        modelPath = "res/objects/rock.glb",
        tileColliderRadius = 3
    ),
    HOUSE(
        modelPath = "res/objects/rock.glb",
        tileColliderRadius = 3
    )
}

@Serializable
sealed interface ObjectProp<V> {

    val v: V


    sealed class PropType<P : ObjectProp<V>, V>(val default: V)

    @Serializable @SerialName("TYPE")
    data class Type(override val v: ObjectType) : ObjectProp<ObjectType> {
        companion object : PropType<Type, ObjectType>(
            default = ObjectType.ROCK
        )
    }
    
    @Serializable @SerialName("POSITION")
    data class Position(override val v: SerVector3) : ObjectProp<SerVector3> {
        companion object : PropType<Position, SerVector3>(
            default = SerVector3(0f, 0f, 0f)
        )
    }

    @Serializable @SerialName("ROTATION")
    data class Rotation(override val v: SerVector3) : ObjectProp<SerVector3> {
        companion object : PropType<Rotation, SerVector3>(
            default = SerVector3(0f, 0f, 0f)
        )
    }

    @Serializable @SerialName("SCALE")
    data class Scale(override val v: Float) : ObjectProp<Float> {
        companion object : PropType<Scale, Float>(default = 1f)
    }
}

@Serializable
data class ObjectInstance(val props: List<ObjectProp<*>>) {

    inline operator fun <reified P : ObjectProp<V>, reified V> get(
        propType: ObjectProp.PropType<P, V>
    ): V
        = this.props.filterIsInstance<P>().firstOrNull()?.v ?: propType.default

}
