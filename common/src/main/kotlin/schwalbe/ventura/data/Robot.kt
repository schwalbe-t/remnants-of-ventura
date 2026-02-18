
package schwalbe.ventura.data

import kotlinx.serialization.Serializable
import org.joml.Vector4f
import org.joml.Vector4fc

@Serializable
enum class RobotStatus(
    val isRunning: Boolean,
    val displayColor: Vector4fc
) {
    STOPPED(
        isRunning = false,
        displayColor = Vector4f(178f, 178f, 178f, 255f).div(255f)
    ),
    RUNNING(
        isRunning = true,
        displayColor = Vector4f(134f, 160f, 099f, 255f).div(255f)
    ),
    PAUSED(
        isRunning = true,
        displayColor = Vector4f(211f, 146f, 091f, 255f).div(255f)
    ),
    ERROR(
        isRunning = false,
        displayColor = Vector4f(186f, 094f, 105f, 255f).div(255f)
    );

    val localNameKey: String
        get() = "RobotStatus:name/${this.name}"
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