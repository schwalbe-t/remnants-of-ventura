
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
    val actions: List<ItemAction>,
    val isRobotAttachment: Boolean
) {
    ROBOT(
        actions = listOf(
            ItemAction.DEPLOY_ROBOT
        ),
        isRobotAttachment = false
    ),
    PROCESSOR(
        actions = listOf(),
        isRobotAttachment = true
    ),
    MEMORY_MODULE(
        actions = listOf(),
        isRobotAttachment = true
    ),
    UTILITY_MODULE(
        actions = listOf(),
        isRobotAttachment = true
    ),
    WEAPON(
        actions = listOf(),
        isRobotAttachment = true
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
    ),

    DIGITAL_RADIO(
        category = ItemCategory.UTILITY_MODULE,
        modelPath = "res/items/radio.glb"
    ),
    GPS_RECEIVER(
        category = ItemCategory.UTILITY_MODULE,
        modelPath = "res/items/gps.glb"
    ),
    SHORT_RANGE_SONAR(
        category = ItemCategory.UTILITY_MODULE,
        modelPath = "res/items/sonar.glb"
    ),
    MID_RANGE_SONAR(
        category = ItemCategory.UTILITY_MODULE,
        modelPath = "res/items/sonar.glb"
    ),
    LONG_RANGE_SONAR(
        category = ItemCategory.UTILITY_MODULE,
        modelPath = "res/items/sonar.glb"
    ),

    LASER(
        category = ItemCategory.WEAPON,
        modelPath = "res/items/laser.glb"
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
    SCOUT_CAMOUFLAGE(
        meshOverrideTexturePaths = "res/robots/scout_camouflage.png"
            .overrides("body")
    ),
    SCOUT_FIREWORKS(
        meshOverrideTexturePaths = "res/robots/scout_fireworks.png"
            .overrides("body")
    ),
    SCOUT_LADYBUG(
        meshOverrideTexturePaths = "res/robots/scout_ladybug.png"
            .overrides("body")
    );

    val localNameKey: String
        get() = "ItemVariant:name/${this.name}"
}


@Serializable
data class Item(
    val type: ItemType,
    val variant: ItemVariant? = null
)