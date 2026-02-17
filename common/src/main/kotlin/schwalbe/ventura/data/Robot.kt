
package schwalbe.ventura.data

import kotlinx.serialization.Serializable

@Serializable
enum class RobotState {
    STOPPED,
    RUNNING,
    PAUSED,
    ERROR
}

@Serializable
data class RobotSummary(
    val state: RobotState,
    val fracHealth: Float,
    val fracMemUsage: Float,
    val fracCpuUsage: Float
)
