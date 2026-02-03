
package schwalbe.ventura.client

import org.joml.Matrix4f
import org.joml.Vector2fc
import org.joml.Vector3f
import org.joml.Vector3fc
import schwalbe.ventura.engine.gfx.ConstFramebuffer

data class Camera(
    val position: Vector3f = Vector3f(0f, 0f, 0f),
    val lookAt: Vector3f = Vector3f(0f, 0f, 0f),
    val up: Vector3f = Vector3f(0f, 1f, 0f),

    var fov: Float = Math.PI.toFloat() / 2f,
    var offsetAngleX: Float = 0f,
    var offsetAngleY: Float = 0f,
    var near: Float = 0.1f,
    var far: Float = 100f
)

fun Camera.computeViewProj(viewport: ConstFramebuffer): Matrix4f = Matrix4f()
    .setPerspectiveOffCenter(
        this.fov,
        this.offsetAngleX, this.offsetAngleY,
        viewport.width.toFloat() / viewport.height.toFloat(),
        this.near, this.far
    )
    .lookAt(this.position, this.lookAt, this.up)

data class CameraRay(val origin: Vector3fc, val dir: Vector3fc)

fun Camera.castRay(
    viewport: ConstFramebuffer, screenPos: Vector2fc
): CameraRay {
    val ndcX: Float = (2f * screenPos.x()) / viewport.width.toFloat() - 1f
    val ndcY: Float = 1f - (2f * screenPos.y()) / viewport.height.toFloat()
    val invViewProj: Matrix4f = this.computeViewProj(viewport).invert()
    val near = Vector3f(ndcX, ndcY, -1f).mulProject(invViewProj)
    val far = Vector3f(ndcX, ndcY, +1f).mulProject(invViewProj)
    return CameraRay(origin = near, dir = far.sub(near).normalize())
}

fun CameraRay.afterDistance(d: Float): Vector3f
    = Vector3f(this.dir).mul(d).add(this.origin)
