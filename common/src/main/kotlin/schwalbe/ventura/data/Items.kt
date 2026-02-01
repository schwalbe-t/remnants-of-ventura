
package schwalbe.ventura.data

import kotlinx.serialization.Serializable
import org.joml.Vector3fc
import org.joml.Vector3f

@Serializable
enum class ItemCategory {
    DEVELOPMENT_ITEM;

    val localNameKey: String
        get() = "ITEM_CATEGORY_NAME/${this.name}"
}


@Serializable
enum class ItemType(
    val category: ItemCategory,
    val modelPath: String,
    val modelCenter: Vector3fc,
    val modelSize: Float
) {
    TEST(
        category = ItemCategory.DEVELOPMENT_ITEM,
        modelPath = "res/player.glb",
        modelCenter = Vector3f(0f, 5.5f, 0f),
        modelSize = 11f
    );

    val localNameKey: String
        get() = "ITEM_TYPE_NAME/${this.name}"
    val localDescKey: String
        get() = "ITEM_TYPE_DESC/${this.name}"
}


private fun String.overrides(
    vararg meshNames: String
): Map<String, String>
        = meshNames.associateBy({ it }, { this })

@Serializable
enum class ItemVariant(
    val meshOverrideTexturePaths: Map<String, String> = mapOf()
) {
    TEST_RED_HOODIE(
        meshOverrideTexturePaths =
            "res/player_red.png"
                .overrides("body", "hair", "eyebrows", "skull")
    );

    val localNameKey: String
        get() = "ITEM_VARIANT_NAME/${this.name}"
}


@Serializable
data class Item(
    val type: ItemType,
    val variant: ItemVariant? = null
)