
package schwalbe.ventura.data

import kotlinx.serialization.Serializable

@Serializable
enum class ItemCategory {
    MISCELLANEOUS;

    val localNameKey: String
        get() = "ITEM_CATEGORY_NAME/${this.name}"
}

@Serializable
enum class ItemType(
    val category: ItemCategory,
    val modelPath: String
) {
    TEST(
        category = ItemCategory.MISCELLANEOUS,
        modelPath = "res/player.glb"
    );

    val localNameKey: String
        get() = "ITEM_TYPE_NAME/${this.name}"
    val localDescKey: String
        get() = "ITEM_TYPE_DESC/${this.name}"
}

@Serializable
enum class ItemVariant(
    val texturePath: String
) {
    TEST_RED_HOODIE(
        texturePath = "res/player_red.png"
    );

    val localNameKey: String
        get() = "ITEM_VARIANT_NAME/${this.name}"
}

@Serializable
data class Item(
    val type: ItemType,
    val variant: ItemVariant? = null
)