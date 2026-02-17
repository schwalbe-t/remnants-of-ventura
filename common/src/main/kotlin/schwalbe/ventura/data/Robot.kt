
package schwalbe.ventura.data

import kotlinx.serialization.Serializable

@Serializable
enum class RobotStatus {
    STOPPED,
    RUNNING,
    PAUSED,
    ERROR
}

@Serializable
enum class RobotType(
    val itemType: ItemType,
    val numAttachments: Int,
    val maxHealth: Float = 100f
) {
    SCOUT(
        itemType = ItemType.KENDAL_DYNAMICS_SCOUT,
        numAttachments = 4
    )
}