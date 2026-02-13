
package schwalbe.ventura.data

import kotlinx.serialization.Serializable
import org.joml.Vector3fc
import org.joml.Vector3f


@Serializable
enum class ItemAction {
    DEPLOY_ROBOT;

    val localNameKey: String
        get() = "ItemAction:name/${this.name}"
}


@Serializable
enum class ItemCategory(
    val actions: List<ItemAction>
) {
    ROBOT(
        actions = listOf(
            ItemAction.DEPLOY_ROBOT
        )
    ),
    PROCESSOR(
        actions = listOf()
    ),
    MEMORY_MODULE(
        actions = listOf()
    );

    val localNameKey: String
        get() = "ItemCategory:name/${this.name}"
}


@Serializable
enum class ItemType(
    val category: ItemCategory,
    val modelPath: String,
    val modelCenter: Vector3fc = Vector3f(0f, +0.5f, 0f),
    val modelSize: Float = 1f
) {
    KENDAL_DYNAMICS_SCOUT(
        category = ItemCategory.ROBOT,
        modelPath = "res/robots/scout.glb"
    ),

    BIGTON_1030(
        category = ItemCategory.PROCESSOR,
        modelPath = "res/items/bigton_1000.glb"
    ),
    BIGTON_1050(
        category = ItemCategory.PROCESSOR,
        modelPath = "res/items/bigton_1000.glb"
    ),
    BIGTON_1070(
        category = ItemCategory.PROCESSOR,
        modelPath = "res/items/bigton_1000.glb"
    ),
    BIGTON_2030(
        category = ItemCategory.PROCESSOR,
        modelPath = "res/items/bigton_2000.glb"
    ),
    BIGTON_2050(
        category = ItemCategory.PROCESSOR,
        modelPath = "res/items/bigton_2000.glb"
    ),
    BIGTON_2070(
        category = ItemCategory.PROCESSOR,
        modelPath = "res/items/bigton_2000.glb"
    ),
    BIGTON_3030(
        category = ItemCategory.PROCESSOR,
        modelPath = "res/items/bigton_3000.glb"
    ),
    BIGTON_3050(
        category = ItemCategory.PROCESSOR,
        modelPath = "res/items/bigton_3000.glb"
    ),
    BIGTON_3070(
        category = ItemCategory.PROCESSOR,
        modelPath = "res/items/bigton_3000.glb"
    ),

    PIVOTAL_ME2048(
        category = ItemCategory.MEMORY_MODULE,
        modelPath = "res/items/pivotal.glb"
    ),
    PIVOTAL_ME5120(
        category = ItemCategory.MEMORY_MODULE,
        modelPath = "res/items/pivotal.glb"
    ),
    PIVOTAL_ME10K(
        category = ItemCategory.MEMORY_MODULE,
        modelPath = "res/items/pivotal.glb"
    ),
    PIVOTAL_ME20K(
        category = ItemCategory.MEMORY_MODULE,
        modelPath = "res/items/pivotal.glb"
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