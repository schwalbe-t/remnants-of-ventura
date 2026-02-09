
package schwalbe.ventura.data

import kotlinx.serialization.Serializable
import org.joml.Vector3fc
import org.joml.Vector3f


@Serializable
enum class ItemAction {
    DEPLOY_ROBOT,
    TEST;

    val localNameKey: String
        get() = "ItemAction:name/${this.name}"
}


@Serializable
enum class ItemCategory(
    val actions: List<ItemAction>
) {
    DEVELOPMENT_ITEM(
        actions = listOf(
            ItemAction.TEST,
            ItemAction.DEPLOY_ROBOT
        )
    );

    val localNameKey: String
        get() = "ItemCategory:name/${this.name}"
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
    ),
    ROCK(
        category = ItemCategory.DEVELOPMENT_ITEM,
        modelPath = "res/objects/rock.glb",
        modelCenter = Vector3f(0f, 1.5f, 0f),
        modelSize = 4.5f
    );

    val localNameKey: String
        get() = "ItemType:name/${this.name}"
    val localDescKey: String
        get() = "ItemType:desc/${this.name}"
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
        get() = "ItemVariant:name/${this.name}"
}


@Serializable
data class Item(
    val type: ItemType,
    val variant: ItemVariant? = null
)