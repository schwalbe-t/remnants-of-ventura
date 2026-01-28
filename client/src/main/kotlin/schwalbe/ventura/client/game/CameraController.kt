
package schwalbe.ventura.client.game

import org.joml.Vector3f
import org.joml.Vector3fc
import schwalbe.ventura.client.Camera
import schwalbe.ventura.engine.input.Mouse
import kotlin.math.PI

class CameraController {

    companion object {
        const val MIN_DISTANCE: Float = 4f
        const val MAX_DISTANCE: Float = 20f
        const val ZOOM_SPEED: Float = 2f // distance per scrolled notch

        val cameraEyeOffsetDir: Vector3fc = Vector3f(0f, +1.75f, +2f).normalize()
        val cameraLookAtOffset: Vector3fc = Vector3f(0f, +1.5f, 0f)
        const val CAMERA_FOV: Float = PI.toFloat() / 4f // 45 degrees

        const val FOLLOW_SPEED: Float = 15f // (distance per second) / distance
    }


    var distance: Float = CameraController.MIN_DISTANCE +
        (CameraController.MAX_DISTANCE - CameraController.MIN_DISTANCE) / 2f
    var position: Vector3f? = null

    fun update(player: Player, camera: Camera, deltaTime: Float) {
        this.distance -= Mouse.scrollOffset.y() * CameraController.ZOOM_SPEED
        this.distance = this.distance.coerceIn(
            CameraController.MIN_DISTANCE, CameraController.MAX_DISTANCE
        )
        camera.fov = CAMERA_FOV
        val targetLookAt = Vector3f(player.position)
            .add(CameraController.cameraLookAtOffset)
        val currPosition: Vector3f = this.position ?: Vector3f(targetLookAt)
        this.position = currPosition
        val toTarget = targetLookAt.sub(currPosition)
        val remDist: Float = toTarget.length()
        if (remDist > 0f) {
            val currSpeed: Float = CameraController.FOLLOW_SPEED * remDist
            val movedDist: Float = minOf(remDist, deltaTime * currSpeed)
            currPosition.add(toTarget.normalize().mul(movedDist))
        }
        camera.lookAt.set(currPosition)
        camera.position
            .set(CameraController.cameraEyeOffsetDir)
            .mul(this.distance)
            .add(camera.lookAt)
    }

}
