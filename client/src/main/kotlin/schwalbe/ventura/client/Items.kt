
package schwalbe.ventura.client

import org.joml.Matrix4fc
import schwalbe.ventura.data.Item
import schwalbe.ventura.data.ItemType
import schwalbe.ventura.data.ItemVariant
import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.gfx.Model
import schwalbe.ventura.engine.gfx.StaticAnim
import schwalbe.ventura.engine.gfx.Texture
import schwalbe.ventura.engine.gfx.loadFile
import schwalbe.ventura.engine.gfx.loadImage
import kotlin.collections.forEach

data class ItemTypeResources(
    val model: Resource<Model<StaticAnim>>
) { companion object {
    val all: List<ItemTypeResources> = ItemType.entries.map {
        ItemTypeResources(
            model = Model.loadFile(
                it.modelPath,
                Renderer.meshProperties,
                textureFilter = Texture.Filter.LINEAR
            )
        )
    }
} }

data class ItemVariantResources(
    val meshTextureOverrides: Map<String, Resource<Texture>>
) {
    companion object {
        val all: List<ItemVariantResources> = ItemVariant.entries.map {
            ItemVariantResources(
                meshTextureOverrides
                    = it.meshOverrideTexturePaths.mapValues { (_, p) ->
                        Texture.loadImage(p, Texture.Filter.LINEAR)
                    }
            )
        }
    }

    fun collectTextureOverrides(): Map<String, Texture>
        = this.meshTextureOverrides.mapValues { it.value() }
}

object Items {
    fun submitResources(loader: ResourceLoader) {
        ItemTypeResources.all.forEach {
            loader.submit(it.model)
        }
        ItemVariantResources.all.forEach {
            it.meshTextureOverrides.values.forEach(loader::submit)
        }
    }

    const val OUTLINE_THICKNESS: Float = 0.015f
}

fun Item.renderOutlined(pass: RenderPass, instances: Iterable<Matrix4fc>) {
    val itemTypeRes = ItemTypeResources.all[this.type.ordinal]
    val itemVariant: ItemVariant? = this.variant
    val itemVariantRes: ItemVariantResources? =
        if (itemVariant == null) { null }
        else { ItemVariantResources.all[itemVariant.ordinal] }
    val itemModel: Model<StaticAnim> = itemTypeRes.model()
    val meshTextureOverrides: Map<String, Texture>?
            = itemVariantRes?.collectTextureOverrides()
    pass.renderOutline(
        itemModel, Items.OUTLINE_THICKNESS, null, instances,
        meshTextureOverrides = meshTextureOverrides
    )
    pass.renderGeometry(
        itemModel, null, instances,
        meshTextureOverrides = meshTextureOverrides
    )
}

