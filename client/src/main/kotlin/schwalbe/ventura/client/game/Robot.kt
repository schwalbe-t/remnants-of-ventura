
package schwalbe.ventura.client.game

import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.client.RenderPass
import schwalbe.ventura.client.Renderer
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.data.*
import schwalbe.ventura.client.ItemVariantResources
import schwalbe.ventura.net.SharedRobotInfo
import org.joml.*
import schwalbe.ventura.client.ItemTypeResources

object RobotAnim : Animations<RobotAnim> {
    val idle = anim("idle")
    val move = anim("move")
}

fun RobotAnim.fromSharedAnim(a: SharedRobotInfo.Animation) = when (a) {
    SharedRobotInfo.Animation.IDLE -> RobotAnim.idle
    SharedRobotInfo.Animation.MOVE -> RobotAnim.move
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

val ROBOT_WEAPON_OFFSETS: Map<ItemType, Vector3fc> = mapOf(
    ItemType.KENDAL_DYNAMICS_SCOUT to Vector3f(0f, +0.25f, 0f)
)

fun Robot.baseTransform(
    pos: Vector3fc, rotY: Float
): Matrix4f = Matrix4f()
    .translate(pos)
    .rotateY(rotY)

fun Robot.weaponTransform(
    baseItemType: ItemType, basePosition: Vector3fc, rotY: Float
): Matrix4f {
    val offset = ROBOT_WEAPON_OFFSETS[baseItemType] ?: Vector3f()
    return Matrix4f()
        .translate(basePosition)
        .translate(offset)
        .rotateY(rotY)
}

fun Robot.render(
    pass: RenderPass, pos: Vector3fc,
    baseRotY: Float, weaponRotY: Float, anim: AnimState<RobotAnim>,
    baseItem: Item, weaponItem: Item?
) {
    val baseTransf = Robot.baseTransform(pos, baseRotY)
    val baseInstances = listOf(baseTransf)
    val baseModel: Model<RobotAnim> = (robotModels[baseItem.type] ?: return)()
    val baseItemVariant: ItemVariant? = baseItem.variant
    val baseTexOverrides = if (baseItemVariant == null) { null } else {
        ItemVariantResources.all[baseItemVariant.ordinal]
            .collectTextureOverrides()
    }
    pass.renderOutline(
        baseModel, Player.OUTLINE_THICKNESS, anim, baseInstances,
        meshTextureOverrides = baseTexOverrides
    )
    pass.renderGeometry(
        baseModel, anim, baseInstances,
        meshTextureOverrides = baseTexOverrides
    )
    if (weaponItem != null) {
        val weaponTransf = Robot.weaponTransform(baseItem.type, pos, weaponRotY)
        val weaponInstances = listOf(weaponTransf)
        val weaponItemTypeRes = ItemTypeResources.all[weaponItem.type.ordinal]
        val weaponItemVar = weaponItem.variant
        val weaponItemVarRes = if (weaponItemVar == null) { null }
            else { ItemVariantResources.all[weaponItemVar.ordinal] }
        val weaponTexOverrides = weaponItemVarRes?.collectTextureOverrides()
        pass.renderOutline(
            weaponItemTypeRes.model(), Player.OUTLINE_THICKNESS, null,
            weaponInstances,
            meshTextureOverrides = weaponTexOverrides
        )
        pass.renderGeometry(
            weaponItemTypeRes.model(), null, weaponInstances,
            meshTextureOverrides = weaponTexOverrides
        )
    }
}