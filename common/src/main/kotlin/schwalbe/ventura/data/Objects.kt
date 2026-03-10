
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
sealed interface ObjectProp<V> {

    val v: V


    sealed interface PropType<P : ObjectProp<V>, V>
    sealed class DefaultPropType<P : ObjectProp<V>, V>(
        val default: V
    ) : PropType<P, V>

    @Serializable
    data class Type(override val v: ObjectType) : ObjectProp<ObjectType> {
        companion object : PropType<Type, ObjectType>
    }
    
    @Serializable
    data class Position(override val v: SerVector3) : ObjectProp<SerVector3> {
        companion object : DefaultPropType<Position, SerVector3>(
            default = SerVector3(0f, 0f, 0f)
        )
    }

    @Serializable
    data class Rotation(override val v: SerVector3) : ObjectProp<SerVector3> {
        companion object : DefaultPropType<Rotation, SerVector3>(
            default = SerVector3(0f, 0f, 0f)
        )
    }

    @Serializable
    data class Scale(override val v: Float) : ObjectProp<Float> {
        companion object : DefaultPropType<Scale, Float>(default = 1f)
    }
}

@Serializable
data class ObjectInstance(val props: List<ObjectProp<*>>) {

    inline operator fun <reified P : ObjectProp<V>, reified V> get(
        propType: ObjectProp.PropType<P, V>
    ): V?
        = this.props.filterIsInstance<P>().firstOrNull()?.v

    inline operator fun <reified P : ObjectProp<V>, reified V> get(
        propType: ObjectProp.DefaultPropType<P, V>
    ): V
        = this.props.filterIsInstance<P>().firstOrNull()?.v ?: propType.default

}
