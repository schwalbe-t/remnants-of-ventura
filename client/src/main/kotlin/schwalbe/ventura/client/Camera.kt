
package schwalbe.ventura.client

import org.joml.Matrix4f
import org.joml.Vector3f
import schwalbe.ventura.engine.gfx.ConstFramebuffer

data class Camera(
    val position: Vector3f = Vector3f(0f, 0f, 0f),
    val lookAt: Vector3f = Vector3f(0f, 0f, 0f),
    val up: Vector3f = Vector3f(0f, 1f, 0f),

    var fov: Float = Math.PI.toFloat() / 2f,
    var near: Float = 0.1f,
    var far: Float = 100f
)

fun Camera.computeViewProj(viewport: ConstFramebuffer): Matrix4f = Matrix4f()
    .setPerspective(
        this.fov,
        viewport.width.toFloat() / viewport.height.toFloat(),
        this.near, this.far
    )
    .lookAt(this.position, this.lookAt, this.up)
