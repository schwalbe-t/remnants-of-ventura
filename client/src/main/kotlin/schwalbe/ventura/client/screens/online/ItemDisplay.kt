
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.Renderer
import schwalbe.ventura.client.ItemTypeResources
import schwalbe.ventura.client.ItemVariantResources
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.data.*
import schwalbe.ventura.engine.ui.MsaaRenderDisplay
import kotlin.math.PI
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Matrix4f

object ItemDisplay {

    val CAMERA_OFFSET: Vector3fc = Vector3f(0f, +1f, +2f).normalize().mul(5f)
    const val CAMERA_FOV: Float = PI.toFloat() / 9f // 180/9 = 20 degrees

    const val OUTLINE_THICKNESS: Float = 0.015f / 2f
    const val ROTATION_TIME_MS: Long = 10_000 // ms / 360 deg
    const val ROTATION_TIME_S: Float
        = ROTATION_TIME_MS.toFloat() / 1000f // s / 360 deg
    const val ROTATION_SPEED: Float
        = (2f * PI.toFloat()) / ROTATION_TIME_S // rad / s

    fun createDisplay(
        item: Item, fixedAngle: Float? = null, msaaSamples: Int = 4
    ): MsaaRenderDisplay {
        val itemTypeRes = ItemTypeResources.all[item.type.ordinal]
        val itemVariant: ItemVariant? = item.variant
        val itemVariantRes: ItemVariantResources? =
            if (itemVariant == null) { null }
            else { ItemVariantResources.all[itemVariant.ordinal] }
        val itemModel: Model<StaticAnim> = itemTypeRes.model()
        val meshTextureOverrides: Map<String, Texture>?
            = itemVariantRes?.collectTextureOverrides()
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
        output.withRenderedContent {
            renderer.update(sunTarget = Vector3f(0f, 0f, 0f))
            renderer.beginShadowPass()
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
        output.withDisposalHandler(renderer::dispose)
        return output
    }

}
