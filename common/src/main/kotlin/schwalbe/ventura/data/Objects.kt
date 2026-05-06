
package schwalbe.ventura.data

import schwalbe.ventura.data.CharacterStylePreset as CharacterStyleP
import schwalbe.ventura.utils.SerVector3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.joml.Matrix4f
import schwalbe.ventura.PaletteColor

// The maximum size in chunks of any client or server side colliders.
// This number determines the radius of the area that is searched when looking
// for colliders that intersect a given point, both on the client and the
// server. Models that are larger than this constant may sometimes not be
// computed properly by client and server collision systems.
const val MAX_COLLIDER_SIZE_CHUNKS: Int = 1

data class ObjectTileCollider(
    val left: Float, val right: Float,
    val top: Float, val bottom: Float
)

@Serializable
enum class ObjectType(
    val modelPath: String = "res/nothing.glb",
    val textureFilter: TextureFilter = TextureFilter.LINEAR,
    val renderOutline: Boolean = true,
    val applyColliders: Boolean = true,
    val applyCollidersOf: Iterable<String>? = null,
    val tileColliderSize: ObjectTileCollider?
) {
    TRIGGER(
        modelPath = "res/objects/trigger.glb",
        renderOutline = false,
        applyColliders = false,
        tileColliderSize = null
    ),
    BUTTON(
        modelPath = "res/objects/button.glb",
        applyColliders = false,
        tileColliderSize = null
    ),
    AND_GATE(
        modelPath = "res/objects/and_gate.glb",
        tileColliderSize = ObjectTileCollider(+0.25f, +0.75f, +0.25f, +0.75f)
    ),
    OR_GATE(
        modelPath = "res/objects/or_gate.glb",
        tileColliderSize = ObjectTileCollider(+0.25f, +0.75f, +0.25f, +0.75f)
    ),
    NAND_GATE(
        modelPath = "res/objects/nand_gate.glb",
        tileColliderSize = ObjectTileCollider(+0.25f, +0.75f, +0.25f, +0.75f)
    ),
    NOR_GATE(
        modelPath = "res/objects/nor_gate.glb",
        tileColliderSize = ObjectTileCollider(+0.25f, +0.75f, +0.25f, +0.75f)
    ),
    LAMP(
        modelPath = "res/objects/lamp.glb",
        tileColliderSize = ObjectTileCollider(-0.25f, +0.25f, -0.25f, +0.25f)
    ),

    ROCK(
        modelPath = "res/objects/rock.glb",
        tileColliderSize = ObjectTileCollider(-1.5f, +1.5f, -1.5f, +1.5f)
    ),
    TREE_GREEN(
        applyCollidersOf = listOf("collider"),
        tileColliderSize = ObjectTileCollider(-0.25f, +0.25f, -0.25f, +0.25f)
    ),
    TREE_ORANGE(
        applyCollidersOf = listOf("collider"),
        tileColliderSize = ObjectTileCollider(-0.25f, +0.25f, -0.25f, +0.25f)
    ),
    TREE_RED(
        applyCollidersOf = listOf("collider"),
        tileColliderSize = ObjectTileCollider(-0.25f, +0.25f, -0.25f, +0.25f)
    ),
    TREE_PINK(
        applyCollidersOf = listOf("collider"),
        tileColliderSize = ObjectTileCollider(-0.25f, +0.25f, -0.25f, +0.25f)
    ),
    LUSH_GRASS(
        applyColliders = false,
        tileColliderSize = null
    ),
    DRY_GRASS(
        applyColliders = false,
        tileColliderSize = null
    ),
    LUSH_BUSH(
        applyColliders = false,
        tileColliderSize = null
    ),
    DRY_BUSH(
        applyColliders = false,
        tileColliderSize = null
    ),
    TUMBLEWEED(
        applyColliders = false,
        tileColliderSize = null
    ),
    CACTUS(
        modelPath = "res/objects/cactus.glb",
        textureFilter = TextureFilter.NEAREST,
        applyCollidersOf = listOf("collider"),
        tileColliderSize = ObjectTileCollider(-0.25f, +0.25f, -0.25f, +0.25f)
    ),

    APARTMENT_CEILING(
        modelPath = "res/objects/apartment_ceiling.glb",
        applyColliders = true,
        tileColliderSize = ObjectTileCollider(-2.5f, +2.5f, -2.5f, +2.5f)
    ),
    APARTMENT_WALL(
        modelPath = "res/objects/apartment_wall.glb",
        applyColliders = true,
        tileColliderSize = null
    ),
    APARTMENT_WINDOWS(
        modelPath = "res/objects/apartment_windows.glb",
        applyColliders = true,
        tileColliderSize = null
    ),
    APARTMENT_BALCONIES(
        modelPath = "res/objects/apartment_balconies.glb",
        applyColliders = true,
        tileColliderSize = ObjectTileCollider(+3.25f, +3.75f, -2.5f, +2.5f)
    ),
    FENCE(
        modelPath = "res/objects/fence.glb",
        applyColliders = true,
        tileColliderSize = ObjectTileCollider(-0.05f, +0.05f, -2.75f, +2.75f)
    ),
    RUINED_WALL(
        modelPath = "res/objects/ruined_wall.glb",
        applyColliders = true,
        tileColliderSize = ObjectTileCollider(-2.75f, +2.75f, -0.05f, +0.05f)
    ),
    RUINS_FLOORS(
        modelPath = "res/objects/ruins_floors.glb",
        applyColliders = true,
        tileColliderSize = ObjectTileCollider(-2.5f, +2.5f, -2.5f, +2.5f)
    ),
    RUINS_WALL(
        modelPath = "res/objects/ruins_wall.glb",
        applyColliders = false,
        tileColliderSize = null
    ),
    ROAD_BLANK(
        modelPath = "res/objects/road_blank.glb",
        applyColliders = false,
        tileColliderSize = null,
        renderOutline = false
    ),
    ROAD_STRAIGHT(
        modelPath = "res/objects/road_straight.glb",
        applyColliders = false,
        tileColliderSize = null,
        renderOutline = false
    ),
    ROAD_CURVE(
        modelPath = "res/objects/road_curve.glb",
        applyColliders = false,
        tileColliderSize = null,
        renderOutline = false
    ),

    CHARACTER(
        applyColliders = false,
        tileColliderSize = null
    );

    enum class TextureFilter {
        NEAREST, LINEAR
    }
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

    @Serializable @SerialName("ENTER_WORLD")
    data class EnterWorldTrigger(override val v: String) : ObjectProp<String> {
        companion object : PropType<EnterWorldTrigger, String>(default = "")
    }

    @Serializable @SerialName("LEAVE_WORLD")
    data class LeaveWorldTrigger(override val v: Unit) : ObjectProp<Unit> {
        companion object : PropType<LeaveWorldTrigger, Unit>(default = Unit)
    }

    @Serializable @SerialName("TRIGGERABLE")
    data class Triggerable(override val v: String) : ObjectProp<String> {
        companion object : PropType<Triggerable, String>(default = "unnamed")
    }

    @Serializable @SerialName("TRIGGER_FOR")
    class TriggerFor(
        override val v: Array<String>
    ) : ObjectProp<Array<String>> {
        companion object : PropType<TriggerFor, Array<String>>(
            default = arrayOf()
        )
    }

    @Serializable @SerialName("CHARACTER_DIALOGUE")
    class CharacterDialogue(
        override val v: String
    ) : ObjectProp<String> {
        companion object : PropType<CharacterDialogue, String>(default = "")
    }

    @Serializable @SerialName("CHARACTER_ANIMATION")
    class CharacterAnimation(
        override val v: SharedPersonAnimation
    ) : ObjectProp<SharedPersonAnimation> {
        companion object : PropType<CharacterAnimation, SharedPersonAnimation>(
            default = SharedPersonAnimation.IDLE
        )
    }

    @Serializable @SerialName("CHARACTER_STYLE_PRESET")
    class CharacterStylePreset(
        override val v: CharacterStyleP
    ) : ObjectProp<CharacterStyleP> {
        companion object : PropType<CharacterStylePreset, CharacterStyleP>(
            default = CharacterStyleP.MERCHANT
        )
    }

    @Serializable @SerialName("CHARACTER_STYLE_CUSTOM")
    class CharacterStyleCustom(
        override val v: Value
    ) : ObjectProp<CharacterStyleCustom.Value> {
        companion object : PropType<CharacterStyleCustom, Value>(
            default = Value(
                colors = List(PersonColorType.entries.size) {
                    PaletteColor.BLACK
                },
                hair = PersonHairStyle.LONG
            )
        )

        @Serializable
        class Value(
            val colors: List<PaletteColor>,
            val hair: PersonHairStyle
        ) {
            fun toPersonStyle() = PersonStyle(
                colors = this.colors.map { it.ser },
                hair = this.hair
            )
        }
    }

}

@Serializable
data class ObjectInstance(val props: List<ObjectProp<*>>) {

    inline operator fun <reified P : ObjectProp<V>, reified V> get(
        propType: ObjectProp.PropType<P, V>
    ): V
        = this.getOrNull(propType) ?: propType.default

    inline fun <reified P : ObjectProp<V>, reified V> getOrNull(
        @Suppress("UNUSED_PARAMETER") propType: ObjectProp.PropType<P, V>
    ): V?
        = this.props.filterIsInstance<P>().firstOrNull()?.v

    inline operator fun <reified P : ObjectProp<V>, reified V> contains(
        @Suppress("UNUSED_PARAMETER") propType: ObjectProp.PropType<P, V>
    ): Boolean
        = this.props.any { it is P }

}

fun ObjectInstance.buildTransform(): Matrix4f {
    val position = this[ObjectProp.Position]
    val rotation = this[ObjectProp.Rotation]
    return Matrix4f()
        .translate(position.x, position.y, position.z)
        .rotateXYZ(rotation.x, rotation.y, rotation.z)
        .scale(this[ObjectProp.Scale])
}

@Serializable
data class ObjectInstanceRef(
    val chunk: ChunkRef,
    val instanceIdx: Int
)