
package schwalbe.ventura.client.game

import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.client.RenderPass
import schwalbe.ventura.client.Renderer
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.data.*
import org.joml.Vector3fc
import schwalbe.ventura.client.ItemVariantResources

object RobotAnim : Animations<RobotAnim> {
    // TODO! replace with actual animations
    val dummy = anim("dummy")
}

val robotModels: Map<ItemType, Resource<Model<RobotAnim>>>
    = ItemType.values()
    .filter { it.category == ItemCategory.ROBOT }
    .associateBy(
        keySelector = { it },
        valueTransform = { Model.loadFile(
            it.modelPath, Renderer.meshProperties, RobotAnim,
            textureFilter = Texture.Filter.LINEAR
        ) }
    )

object Robot {
    fun submitResources(loader: ResourceLoader) {
        robotModels.values.forEach(loader::submit)
    }
}

fun Robot.render(
    pass: RenderPass, pos: Vector3fc, rotY: Float, anim: AnimState<RobotAnim>,
    item: Item
) {
    val transf = Player.modelTransform(pos, rotY)
    val instances = listOf(transf)
    val model: Model<RobotAnim> = (robotModels[item.type] ?: return)()
    val variant: ItemVariant? = item.variant
    val texOverrides = if (variant == null) { null } else {
        ItemVariantResources.all[variant.ordinal].collectTextureOverrides()
    }
    pass.renderOutline(
        model, Player.OUTLINE_THICKNESS, anim, instances,
        meshTextureOverrides = texOverrides
    )
    pass.renderGeometry(
        model, anim, instances,
        meshTextureOverrides = texOverrides
    )
}