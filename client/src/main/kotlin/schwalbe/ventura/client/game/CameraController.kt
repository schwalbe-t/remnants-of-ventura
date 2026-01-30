
package schwalbe.ventura.client.game

import org.joml.Vector3f
import org.joml.Vector3fc
import schwalbe.ventura.client.Camera
import schwalbe.ventura.client.Client
import schwalbe.ventura.engine.gfx.ConstFramebuffer
import schwalbe.ventura.engine.input.Mouse
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.tan

private fun computeAspect(fb: ConstFramebuffer): Float
    = fb.width.toFloat() / fb.height.toFloat()

private fun computeHalfVert(fov: Float): Float
    = fov * 0.5f

private fun computeHalfHoriz(halfVert: Float, aspect: Float): Float
    = atan(tan(halfVert) * aspect)

class CameraController {

    enum class Mode(
        val lookAt: (Renderer, World, CameraController) -> Vector3f,
        fovDegrees: Float,
        val offsetAngleX: (Renderer, Float, Float) -> Float = { _, _, _ -> 0f },
        val offsetAngleY: (Renderer, Float, Float) -> Float = { _, _, _ -> 0f },
        val distance: (CameraController) -> Float = { c -> c.distance }
    ) {
        PLAYER_AT_CENTER(
            lookAt = { _, w, _ -> Vector3f()
                .add(w.player.position)
                .add(0f, +1.5f, 0f)
            },
            fovDegrees = 45f
        ),
        PLAYER_ON_SIDE(
            lookAt = { _, w, _ -> Vector3f()
                .add(w.player.position)
                .add(0f, +1.5f, 0f)
            },
            fovDegrees = 20f,
            offsetAngleX = { _, hh, _ -> atan(tan(hh) * -2f/3f) },
            distance = { _ -> 10f }
        );

        val fovRadians: Float = fovDegrees * PI.toFloat() / 180f

        private fun computeOffsetAngle(
            renderer: Renderer, f: (Renderer, Float, Float) -> Float
        ): Float {
            val aspect: Float = computeAspect(renderer.dest)
            val halfVert: Float = computeHalfVert(this.fovRadians)
            val halfHoriz: Float = computeHalfHoriz(halfVert, aspect)
            return f(renderer, halfHoriz, halfVert)
        }

        fun computeOffsetAngleX(renderer: Renderer): Float
            = this.computeOffsetAngle(renderer, this.offsetAngleX)

        fun computeOffsetAngleY(renderer: Renderer): Float
            = this.computeOffsetAngle(renderer, this.offsetAngleY)
    }

    companion object {
        const val MIN_DISTANCE: Float = 4f
        const val MAX_DISTANCE: Float = 20f
        const val ZOOM_SPEED: Float = 2f // distance per scrolled notch

        val cameraEyeOffsetDir: Vector3fc = Vector3f(0f, +1.75f, +2f).normalize()

        const val FOLLOW_SPEED: Float = 15f // (distance per second) / distance
    }


    var mode: Mode = Mode.PLAYER_ON_SIDE
    var distance: Float = CameraController.MIN_DISTANCE +
        (CameraController.MAX_DISTANCE - CameraController.MIN_DISTANCE) / 2f
    var position: Vector3f? = null

    fun update(camera: Camera, client: Client, world: World) {
        this.distance -= Mouse.scrollOffset.y() * CameraController.ZOOM_SPEED
        this.distance = this.distance.coerceIn(
            CameraController.MIN_DISTANCE, CameraController.MAX_DISTANCE
        )
        val targetLookAt: Vector3f
            = this.mode.lookAt(client.renderer, world, this)
        val currPosition: Vector3f = this.position ?: Vector3f(targetLookAt)
        this.position = currPosition
        val toTarget = targetLookAt.sub(currPosition)
        val remDist: Float = toTarget.length()
        if (remDist > 0f) {
            val currSpeed: Float = CameraController.FOLLOW_SPEED * remDist
            val movedDist: Float = minOf(remDist, client.deltaTime * currSpeed)
            currPosition.add(toTarget.normalize().mul(movedDist))
        }
        val dispDistance: Float = this.mode.distance(this)
        camera.lookAt.set(currPosition)
        camera.position
            .set(CameraController.cameraEyeOffsetDir)
            .mul(dispDistance)
            .add(camera.lookAt)
        camera.fov = this.mode.fovRadians
        camera.offsetAngleX = this.mode.computeOffsetAngleX(client.renderer)
        camera.offsetAngleY = this.mode.computeOffsetAngleY(client.renderer)
    }

}
