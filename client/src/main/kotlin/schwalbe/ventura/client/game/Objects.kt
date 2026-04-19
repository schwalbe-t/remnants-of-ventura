
package schwalbe.ventura.client.game

import schwalbe.ventura.client.RenderPass
import schwalbe.ventura.data.ObjectInstance
import schwalbe.ventura.client.game.ChunkLoader.Companion.objectModels
import schwalbe.ventura.data.ObjectProp
import schwalbe.ventura.data.ObjectType
import schwalbe.ventura.data.buildTransform
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.Resource
import org.joml.Matrix4fc
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector3fc
import schwalbe.ventura.client.OutlineVert

interface ObjectStateProvider {
    fun isTriggered(obj: ObjectInstance): Boolean
}

abstract class ObjectOverrides {
    open fun submitResources(loader: ResourceLoader) {}

    open fun update(state: ObjectStateProvider, obj: ObjectInstance) {}

    open fun transform(
        state: ObjectStateProvider, obj: ObjectInstance
    ): Matrix4f
        = obj.buildTransform()

    sealed interface RenderMethod {
        companion object {
            val DEFAULT: RenderMethod = Batched { p, _, type, transf ->
                val model: Model<StaticAnim> = objectModels
                    .getOrNull(type.ordinal)?.invoke() ?: return@Batched
                Objects.renderBatch(p, type.renderOutline, model, transf)
            }
        }

        class Single(val f: (
            pass: RenderPass, state: ObjectStateProvider,
            obj: ObjectInstance, transform: Matrix4fc
        ) -> Unit) : RenderMethod

        class Batched(val f: (
            pass: RenderPass, state: ObjectStateProvider,
            objType: ObjectType, transforms: Iterable<Matrix4fc>
        ) -> Unit) : RenderMethod
    }

    open val render: RenderMethod = RenderMethod.DEFAULT
}

object Objects {

    const val OUTLINE_THICKNESS: Float = 0.015f

    fun submitResources(resLoader: ResourceLoader) {
        this.OVERRIDES.values.forEach { it.submitResources(resLoader) }
    }

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
        model: Model<StaticAnim>, instances: Iterable<Matrix4fc>,
        faceCulling: FaceCulling = FaceCulling.DISABLED,
        depthTesting: DepthTesting = DepthTesting.ENABLED,
        renderedMeshes: Collection<String>? = null,
        meshTextureOverrides: Map<String, Texture>? = null,
        shadowFactorOverride: Vector3fc? = null,
        outlineFactorOverride: Vector3fc? = null
    ) {
        if (renderOutline) {
            pass.renderOutline(
                model, this.OUTLINE_THICKNESS, animState = null, instances,
                depthTesting, renderedMeshes, meshTextureOverrides,
                outlineFactorOverride
            )
        }
        pass.renderGeometry(
            model, animState = null, instances,
            faceCulling, depthTesting, renderedMeshes, meshTextureOverrides,
            shadowFactorOverride
        )
    }

    fun renderMethodOf(objType: ObjectType): ObjectOverrides.RenderMethod
        = OVERRIDES[objType]?.render ?: ObjectOverrides.RenderMethod.DEFAULT

    fun renderMethodOf(obj: ObjectInstance): ObjectOverrides.RenderMethod
        = this.renderMethodOf(obj[ObjectProp.Type])

    val OVERRIDES: Map<ObjectType, ObjectOverrides> = mapOf(
        ObjectType.BUTTON to ButtonOverrides,
        ObjectType.LAMP to LampOverrides
    )

}

private object ButtonOverrides : ObjectOverrides() {

    const val HEAD_TRIGGER_OFFSET: Float = -0.08f

    override val render = RenderMethod.Single { pass, state, obj, transform ->
        val model: Model<StaticAnim> = objectModels
            .getOrNull(ObjectType.BUTTON.ordinal)?.invoke() ?: return@Single
        val isDown: Boolean = state.isTriggered(obj)
        val headTransf: Matrix4fc = Matrix4f(transform)
            .translateLocal(0f, if (isDown) HEAD_TRIGGER_OFFSET else 0f, 0f)
        Objects.renderBatch(
            pass, renderOutline = true, model, instances = listOf(transform),
            renderedMeshes = listOf("button_base")
        )
        Objects.renderBatch(
            pass, renderOutline = true, model, instances = listOf(headTransf),
            renderedMeshes = listOf("button_head")
        )
    }

}

private object LampOverrides : ObjectOverrides() {

    val lampOffTexture: Resource<Texture>
        = Texture.loadImage("res/objects/lamp_off.png", Texture.Filter.NEAREST)
    val lampOnTexture: Resource<Texture>
        = Texture.loadImage("res/objects/lamp_on.png", Texture.Filter.NEAREST)

    override fun submitResources(loader: ResourceLoader)
        = loader.submitAll(this.lampOffTexture, this.lampOnTexture)

    override val render = RenderMethod.Single { pass, state, obj, transform ->
        val model: Model<StaticAnim> = objectModels
            .getOrNull(ObjectType.LAMP.ordinal)?.invoke() ?: return@Single
        val instances = listOf(transform)
        val isOn: Boolean = state.isTriggered(obj)
        val tex: Texture
                = if (isOn) this.lampOnTexture() else this.lampOffTexture()
        val texOverrides: Map<String, Texture> = mapOf(
            "lamp_base" to tex, "lamp_filament" to tex, "lamp_glass" to tex
        )
        val litShadowFactor = if (isOn) Vector3f(1f, 1f, 1f) else null
        Objects.renderBatch(
            pass, renderOutline = true, model, instances,
            renderedMeshes = listOf("lamp_base"),
            meshTextureOverrides = texOverrides
        )
        Objects.renderBatch(
            pass, renderOutline = true, model, instances,
            renderedMeshes = listOf("lamp_filament"),
            meshTextureOverrides = texOverrides,
            shadowFactorOverride = litShadowFactor
        )
        pass.renderGeometry(
            model, animState = null, instances,
            faceCulling = FaceCulling.BACK,
            renderedMeshes = listOf("lamp_glass"),
            meshTextureOverrides = texOverrides,
            shadowFactorOverride = litShadowFactor
        )
    }

}
