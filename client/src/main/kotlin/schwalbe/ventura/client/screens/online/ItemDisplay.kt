
package schwalbe.ventura.client.screens.online

import org.joml.Matrix4f
import schwalbe.ventura.client.Renderer
import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.data.*
import schwalbe.ventura.engine.ui.MsaaRenderDisplay
import kotlin.math.PI
import org.joml.Vector3f
import org.joml.Vector3fc

data class ItemTypeResources(
    val model: Resource<Model<StaticAnim>>
)

fun ItemTypeResources.submitResources(loader: ResourceLoader) {
    loader.submit(this.model)
}

private val itemTypeResources: List<ItemTypeResources>
    = ItemType.entries.map { ItemTypeResources(
        model = Model.loadFile(
            it.modelPath,
            Renderer.meshProperties,
            textureFilter = Texture.Filter.LINEAR
        )
    ) }


data class ItemVariantResources(
    val meshTextureOverrides: Map<String, Resource<Texture>>
)

fun ItemVariantResources.submitResources(loader: ResourceLoader) {
    this.meshTextureOverrides.values.forEach(loader::submit)
}

private val itemVariantResources: List<ItemVariantResources>
    = ItemVariant.entries.map { ItemVariantResources(
        meshTextureOverrides = it.meshOverrideTexturePaths
            .mapValues { (_, p) -> Texture.loadImage(p, Texture.Filter.LINEAR) }
    ) }


object ItemDisplay {

    fun submitResources(loader: ResourceLoader) {
        itemTypeResources.forEach { it.submitResources(loader) }
        itemVariantResources.forEach { it.submitResources(loader) }
    }

    val CAMERA_OFFSET: Vector3fc = Vector3f(0f, +1f, +2f).normalize().mul(2f)
    const val CAMERA_FOV: Float = PI.toFloat() / 6f // 180/6 = 30 degrees

    const val OUTLINE_THICKNESS: Float = 0.015f / 2f
    const val ROTATION_TIME_MS: Long = 10_000 // ms / 360 deg
    const val ROTATION_TIME_S: Float
        = ROTATION_TIME_MS.toFloat() / 1000f // s / 360 deg
    const val ROTATION_SPEED: Float
        = (2f * PI.toFloat()) / ROTATION_TIME_S // rad / s

    fun createDisplay(
        item: Item, fixedAngle: Float? = null, msaaSamples: Int = 4
    ): MsaaRenderDisplay {
        val itemTypeRes = itemTypeResources[item.type.ordinal]
        val itemVariant: ItemVariant? = item.variant
        val itemVariantRes: ItemVariantResources? =
            if (itemVariant == null) { null }
            else { itemVariantResources[itemVariant.ordinal] }
        val itemModel: Model<StaticAnim> = itemTypeRes.model()
        val meshTextureOverrides: Map<String, Texture>?
            = itemVariantRes?.meshTextureOverrides?.mapValues { (_, t) -> t() }
        val output = MsaaRenderDisplay(samples = msaaSamples)
        val renderer = Renderer(output.msaaTarget)
        renderer.camera.lookAt
            .set(item.type.modelCenter)
            .div(item.type.modelSize)
        renderer.camera.position
            .set(renderer.camera.lookAt)
            .add(CAMERA_OFFSET)
        renderer.camera.fov = CAMERA_FOV
        val startTimeMs: Long = System.currentTimeMillis()
        return output.withRenderedContent {
            renderer.update(sunTarget = Vector3f(0f, 0f, 0f))
            val pass = renderer.beginGeometryPass()
            val absTimeMs: Long = System.currentTimeMillis()
            val timeMs: Long = (absTimeMs - startTimeMs) % ROTATION_TIME_MS
            val timeS: Float = timeMs.toFloat() / 1000f
            val angleY: Float = fixedAngle ?: (timeS * ROTATION_SPEED)
            val instances = listOf(Matrix4f()
                .rotateY(angleY)
                .scale(1f / item.type.modelSize)
            )
            pass.renderOutline(
                itemModel, OUTLINE_THICKNESS, null, instances,
                meshTextureOverrides = meshTextureOverrides
            )
            pass.renderGeometry(
                itemModel, null, instances,
                meshTextureOverrides = meshTextureOverrides
            )
        }
    }

}
