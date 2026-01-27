
package schwalbe.ventura.client.game

import schwalbe.ventura.client.*
import schwalbe.ventura.engine.*
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.engine.input.*
import kotlin.math.*
import org.joml.*

object PlayerAnim : Animations<PlayerAnim> {
    val idle = anim("idle")
    val walk = anim("walk")
    val squat = anim("squat")
}

val playerModel: Resource<Model<PlayerAnim>> = Model.loadFile(
    "res/player.glb",
    Renderer.meshProperties, PlayerAnim,
    textureFilter = Texture.Filter.LINEAR
)

private fun xzVectorAngle(a: Vector3fc, b: Vector3fc): Float
    = atan2(
        (a.z() * b.x()) - (a.x() * b.z()),
        (a.x() * b.x()) + (a.z() * b.z())
    )

class Player {

    companion object {
        fun submitResources(loader: ResourceLoader) = loader.submitAll(
            playerModel
        )

        const val MODEL_SCALE: Float = 1/5.5f
        val modelNoRotationDir: Vector3fc = Vector3f(0f, 0f, +1f)
        val up: Vector3fc = Vector3f(0f, +1f, 0f)

        val cameraEyeOffset: Vector3fc = Vector3f(0f, +3f, +2f)
        val cameraLookAtOffset: Vector3fc = Vector3f(0f, +1f, 0f)

        const val WALK_SPEED: Float = 3f
        const val ROTATION_SPEED: Float = 2f * PI.toFloat() // radians per second
    }

    private var wasMoving: Boolean = false
    val anim = AnimState(PlayerAnim.idle)

    val position: Vector3f = Vector3f()
    val direction: Vector3f = Vector3f(Player.modelNoRotationDir)

    fun update(client: Client) {
        val velocity = Vector3f()
        if (Key.W.isPressed) { velocity.z -= 1f; }
        if (Key.S.isPressed) { velocity.z += 1f; }
        if (Key.A.isPressed) { velocity.x -= 1f; }
        if (Key.D.isPressed) { velocity.x += 1f; }
        if (velocity.length() != 0f) {
            val newDir: Vector3f = velocity.normalize(Vector3f())
            velocity.set(newDir).mul(Player.WALK_SPEED).mul(client.deltaTime)
            this.position.add(velocity)
            var rotationDiff = xzVectorAngle(this.direction, newDir)
            val remTime: Float = abs(rotationDiff) / Player.ROTATION_SPEED
            if (client.deltaTime <= remTime) {
                this.direction
                    .lerp(newDir, client.deltaTime / remTime)
                    .normalize()
            } else {
                this.direction.set(newDir)
            }
            if (!this.wasMoving) {
                this.anim.flushTransitions()
                this.anim.transitionTo(PlayerAnim.walk, 0.25f)
                this.wasMoving = true
            }
        } else if (this.wasMoving) {
            this.anim.flushTransitions()
            this.anim.transitionTo(PlayerAnim.idle, 0.25f)
            this.wasMoving = false
        }
        val camera = client.renderer.camera
        camera.position.set(this.position).add(Player.cameraEyeOffset)
        camera.lookAt.set(this.position).add(Player.cameraLookAtOffset)
        this.anim.addTimePassed(client.deltaTime)
    }

    fun render(client: Client) {
        var rotation = xzVectorAngle(Player.modelNoRotationDir, this.direction)
        val transf = Matrix4f()
            .translate(this.position)
            .rotateY(rotation)
            .scale(Player.MODEL_SCALE)
        client.renderer.render(
            playerModel(),
            geometryShader(), GeometryVert.renderer, GeometryFrag.renderer,
            this.anim, listOf(transf)
        )
    }

}