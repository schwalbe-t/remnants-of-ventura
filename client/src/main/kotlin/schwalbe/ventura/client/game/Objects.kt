
package schwalbe.ventura.client.game

import schwalbe.ventura.client.RenderPass
import schwalbe.ventura.data.ObjectInstance
import schwalbe.ventura.client.game.ChunkLoader.Companion.objectModels
import schwalbe.ventura.data.ObjectProp
import schwalbe.ventura.data.ObjectType
import schwalbe.ventura.data.buildTransform
import schwalbe.ventura.engine.gfx.Model
import schwalbe.ventura.engine.gfx.StaticAnim
import org.joml.Matrix4fc
import org.joml.Matrix4f

interface ObjectStateProvider {
    fun isTriggered(obj: ObjectInstance): Boolean
}

interface ObjectOverrides {
    fun update(state: ObjectStateProvider, obj: ObjectInstance) {}

    fun transform(
        state: ObjectStateProvider, obj: ObjectInstance
    ): Matrix4f
        = obj.buildTransform()

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

    val OVERRIDES: Map<ObjectType, ObjectOverrides> = mapOf()

}
