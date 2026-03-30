
package schwalbe.ventura.client.game

import schwalbe.ventura.client.RenderPass
import schwalbe.ventura.data.ObjectInstance
import org.joml.Matrix4fc
import org.joml.Matrix4f
import schwalbe.ventura.client.game.ChunkLoader.Companion.objectModels
import schwalbe.ventura.data.ObjectProp
import schwalbe.ventura.data.ObjectType
import schwalbe.ventura.engine.gfx.Model
import schwalbe.ventura.engine.gfx.StaticAnim

interface ObjectStateProvider {
    fun isTriggered(obj: ObjectInstance): Boolean
}

interface ObjectOverrides {
    fun update(state: ObjectStateProvider, obj: ObjectInstance) {}

    fun transform(
        state: ObjectStateProvider, obj: ObjectInstance
    ): Matrix4f
        = Objects.baseTransform(obj)

    fun render(
        pass: RenderPass, state: ObjectStateProvider, obj: ObjectInstance,
        transform: Matrix4fc
    ) {
        val type: ObjectType = obj[ObjectProp.Type]
        val model: Model<StaticAnim> = objectModels
            .getOrNull(type.ordinal)?.invoke() ?: return
        Objects.renderBatch(pass, type.renderOutline, model, listOf(transform))
    }
}

object Objects {

    const val OUTLINE_THICKNESS: Float = 0.015f

    fun update(state: ObjectStateProvider, obj: ObjectInstance) {
        val overrides = this.OVERRIDES[obj[ObjectProp.Type]] ?: return
        overrides.update(state, obj)
    }

    fun baseTransform(obj: ObjectInstance): Matrix4f {
        val position = obj[ObjectProp.Position]
        val rotation = obj[ObjectProp.Rotation]
        return Matrix4f()
            .translate(position.x, position.y, position.z)
            .rotateXYZ(rotation.x, rotation.y, rotation.z)
            .scale(obj[ObjectProp.Scale])
    }
    fun transform(state: ObjectStateProvider, obj: ObjectInstance): Matrix4f? {
        val overrides = this.OVERRIDES[obj[ObjectProp.Type]] ?: return null
        return overrides.transform(state, obj)
    }

    fun renderBatch(
        pass: RenderPass, renderOutline: Boolean,
        model: Model<StaticAnim>, instances: Iterable<Matrix4fc>
    ) {
        if (renderOutline) {
            pass.renderOutline(
                model, this.OUTLINE_THICKNESS, animState = null, instances
            )
        }
        pass.renderGeometry(model, animState = null, instances)
    }
    inline fun render(
        pass: RenderPass, state: ObjectStateProvider, obj: ObjectInstance,
        transform: Matrix4fc,
        crossinline otherwise: () -> Unit
    ) {
        val overrides = this.OVERRIDES[obj[ObjectProp.Type]]
            ?: return otherwise()
        overrides.render(pass, state, obj, transform)
    }

    val OVERRIDES: Map<ObjectType, ObjectOverrides> = mapOf(
        ObjectType.ROCK to RockOverrides
    )

}

object RockOverrides : ObjectOverrides {
    override fun transform(
        state: ObjectStateProvider, obj: ObjectInstance
    ): Matrix4f {
        val a: Double = System.currentTimeMillis().toDouble() / 1000.0 % 6.28
        return Objects.baseTransform(obj)
            .rotateY(a.toFloat())
    }
}
