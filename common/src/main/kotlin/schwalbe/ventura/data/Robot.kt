
package schwalbe.ventura.data

import kotlinx.serialization.Serializable
import org.joml.Vector4f
import org.joml.Vector4fc

@Serializable
enum class RobotStatus(
    val isRunning: Boolean,
    val displayColor: Vector4fc
) {
    STOPPED(isRunning = false, displayColor = StatusColor.GRAY),
    RUNNING(isRunning = true, displayColor = StatusColor.GREEN),
    PAUSED(isRunning = true, displayColor = StatusColor.YELLOW),
    ERROR(isRunning = false, displayColor = StatusColor.RED);

    object StatusColor {
        val GRAY: Vector4fc     = Vector4f(178f, 178f, 178f, 255f).div(255f)
        val RED: Vector4fc      = Vector4f(255f, 128f, 128f, 255f).div(255f)
        val YELLOW: Vector4fc   = Vector4f(255f, 255f, 128f, 255f).div(255f)
        val GREEN: Vector4fc    = Vector4f(128f, 255f, 128f, 255f).div(255f)
    }

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