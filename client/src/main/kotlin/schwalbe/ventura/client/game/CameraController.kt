
package schwalbe.ventura.client.game

import schwalbe.ventura.client.Camera
import schwalbe.ventura.client.Client
import schwalbe.ventura.client.Renderer
import schwalbe.ventura.engine.gfx.ConstFramebuffer
import schwalbe.ventura.engine.input.Mouse
import schwalbe.ventura.utils.*
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.tan
import org.joml.Vector3f
import org.joml.Vector3fc

private fun computeAspect(fb: ConstFramebuffer): Float
    = fb.width.toFloat() / fb.height.toFloat()

private fun computeHalfVert(fov: Float): Float
    = fov * 0.5f

private fun computeHalfHoriz(halfVert: Float, aspect: Float): Float
    = atan(tan(halfVert) * aspect)

class CameraController {

    class Mode(
        val lookAt: (Renderer, World, CameraController) -> Vector3f,
        fovDegrees: Float,
        val offsetAngleX: (Renderer, Float, Float) -> Float = { _, _, _ -> 0f },
        val offsetAngleY: (Renderer, Float, Float) -> Float = { _, _, _ -> 0f },
        val distance: (CameraController) -> Float = { c -> c.userDistance }
    ) {
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
        const val MIN_DISTANCE: Float = 6f
        const val MAX_DISTANCE: Float = 30f
        const val START_DISTANCE: Float = 18f
        const val ZOOM_SPEED: Float = 2f // distance per scrolled notch
        val CAMERA_EYE_OFFSET_DIR: Vector3fc
            = Vector3f(0f, +1.75f, +2f).normalize()
        const val POSITION_RESPONSE: Float = 15f
        const val POSITION_EPSILON: Float = 0.01f

        val PLAYER_AT_CENTER = Mode(
            lookAt = { _, w, _ -> Vector3f()
                .add(w.player.position)
                .add(0f, +1f, 0f)
            },
            fovDegrees = 30f
        )
    }


    var mode: Mode = PLAYER_AT_CENTER
    var userDistance: Float = START_DISTANCE

    var lookAt: SmoothedVector3f? = null
    val distance: SmoothedFloat = START_DISTANCE
        .smoothed(response = 10f, epsilon = 0.01f)
    val fov: SmoothedFloat = this.mode.fovRadians
        .smoothed(response = 10f, epsilon = 0.0001f)
    val offsetAngleX: SmoothedFloat = 0f
        .smoothed(response = 10f, epsilon = 0.0001f)
    val offsetAngleY: SmoothedFloat = 0f
        .smoothed(response = 10f, epsilon = 0.0001f)

    fun update(
        camera: Camera, client: Client, world: World, captureInput: Boolean
    ) {
        if (captureInput) {
            this.userDistance -= Mouse.scrollOffset.y() * ZOOM_SPEED
            this.userDistance = this.userDistance
                .coerceIn(MIN_DISTANCE, MAX_DISTANCE)
        }
        val m: Mode = this.mode
        val targetLookAt: Vector3f = m.lookAt(client.renderer, world, this)
        val lookAt: SmoothedVector3f = this.lookAt
            ?: Vector3f(targetLookAt)
                .smoothed(POSITION_RESPONSE, POSITION_EPSILON)
        this.lookAt = lookAt
        lookAt.target.set(targetLookAt)
        this.distance.target = m.distance(this)
        this.fov.target = m.fovRadians
        this.offsetAngleX.target = m.computeOffsetAngleX(client.renderer)
        this.offsetAngleY.target = m.computeOffsetAngleY(client.renderer)
        lookAt.update()
        this.distance.update()
        this.fov.update()
        this.offsetAngleX.update()
        this.offsetAngleY.update()
        camera.lookAt.set(lookAt.value)
        camera.position
            .set(CAMERA_EYE_OFFSET_DIR)
            .mul(this.distance.value)
            .add(camera.lookAt)
        camera.fov = this.fov.value
        camera.offsetAngleX = this.offsetAngleX.value
        camera.offsetAngleY = this.offsetAngleY.value
    }

}
