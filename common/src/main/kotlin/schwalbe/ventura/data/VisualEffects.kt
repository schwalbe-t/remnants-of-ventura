
package schwalbe.ventura.data

import kotlinx.serialization.Serializable
import schwalbe.ventura.net.SerVector3
import kotlin.uuid.Uuid

@Serializable
sealed interface VisualEffect {

    companion object {
        const val MAX_BROADCAST_DIST: Float = 100f
    }

    @Serializable
    data class LaserRay(
        val originRobot: Uuid, val towards: SerVector3
    ) : VisualEffect

}
