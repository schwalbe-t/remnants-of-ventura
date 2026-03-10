
package schwalbe.ventura.client.screens.online

import org.joml.Vector3f
import schwalbe.ventura.client.game.CameraController
import schwalbe.ventura.client.game.Player
import kotlin.math.atan
import kotlin.math.tan

object CameraModes {

    fun playerFarCentered(player: Player) = CameraController.Mode(
        lookAt = { _ -> Vector3f()
            .add(player.position)
            .add(0f, +1.25f, 0f)
        },
        fovDegrees = 20f,
        distance = { _ -> 10f }
    )

    fun playerInRightHalf(player: Player) = CameraController.Mode(
        lookAt = { _ -> Vector3f()
            .add(player.position)
            .add(0f, +1.25f, 0f)
        },
        fovDegrees = 20f,
        offsetAngleX = { _, hh, _ -> atan(tan(hh) * -1f/2f) },
        distance = { _ -> 10f }
    )

    fun playerInRightThird(player: Player) = CameraController.Mode(
        lookAt = { _ -> Vector3f()
            .add(player.position)
            .add(0f, +1.25f, 0f)
        },
        fovDegrees = 20f,
        offsetAngleX = { _, hh, _ -> atan(tan(hh) * -2f/3f) },
        distance = { _ -> 10f }
    )

}
