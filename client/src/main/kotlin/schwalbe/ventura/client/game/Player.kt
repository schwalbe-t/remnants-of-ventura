
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

private fun wrapAngle(angle: Float): Float {
    var a = angle
    while (a > PI)  a -= (2f * PI.toFloat())
    while (a < -PI) a += (2f * PI.toFloat())
    return a
}

class Player {

    companion object {
        fun submitResources(loader: ResourceLoader) = loader.submitAll(
            playerModel
        )

        const val MODEL_SCALE: Float = 1/5.5f
        val modelNoRotationDir: Vector3fc = Vector3f(0f, 0f, +1f)

        const val WALK_SPEED: Float = 3f
        const val ROTATION_SPEED: Float = 2f * PI.toFloat() * 1.5f // radians per second

        const val OUTLINE_THICKNESS: Float = 0.015f

        val relCollider: AxisAlignedBox = axisBoxOf(
            Vector3f(-0.125f, 0f, -0.125f),
            Vector3f(+0.125f, 1f, +0.125f)
        )
    }

    private var wasMoving: Boolean = false
    val anim = AnimState(PlayerAnim.idle)

    val position: Vector3f = Vector3f()
    var rotation: Float = 0f

    fun update(client: Client) {
        val world: World = client.world ?: return
        val collidingBefore: Boolean = world.chunks
            .intersectsAnyLoaded(Player.relCollider.translate(this.position))
        val velocity = Vector3f()
        if (Key.W.isPressed) { velocity.z -= 1f; }
        if (Key.S.isPressed) { velocity.z += 1f; }
        if (Key.A.isPressed) { velocity.x -= 1f; }
        if (Key.D.isPressed) { velocity.x += 1f; }
        if (velocity.length() != 0f) {
            velocity.normalize()
            var targetRot = xzVectorAngle(Player.modelNoRotationDir, velocity)
            velocity.mul(Player.WALK_SPEED).mul(client.deltaTime)
            val newPosX = this.position.add(velocity.x(), 0f, 0f, Vector3f())
            val newPosZ = this.position.add(0f, 0f, velocity.z(), Vector3f())
            val collidingAfterX: Boolean = world.chunks
                .intersectsAnyLoaded(Player.relCollider.translate(newPosX))
            val collidingAfterZ: Boolean = world.chunks
                .intersectsAnyLoaded(Player.relCollider.translate(newPosZ))
            if (!collidingAfterX || collidingBefore) {
                this.position.add(velocity.x(), 0f, 0f)
            }
            if (!collidingAfterZ || collidingBefore) {
                this.position.add(0f, 0f, velocity.z())
            }
            val rToTarget: Float = wrapAngle(targetRot - this.rotation)
            val rotDist: Float = Player.ROTATION_SPEED * client.deltaTime
            this.rotation += sign(rToTarget) * minOf(abs(rToTarget), rotDist)
            if (!this.wasMoving) {
                this.anim.transitionTo(PlayerAnim.walk, 0.25f)
                this.wasMoving = true
            }
        } else if (this.wasMoving) {
            this.anim.transitionTo(PlayerAnim.idle, 0.25f)
            this.wasMoving = false
        }
        this.anim.addTimePassed(client.deltaTime)
        this.anim.addTransitionTimePassed(
            client.deltaTime * this.anim.numQueuedTransitions
        )
    }

    fun render(client: Client) {
        val transf = Matrix4f()
            .translate(this.position)
            .rotateY(this.rotation)
            .scale(Player.MODEL_SCALE)
        val instances = listOf(transf)
        client.renderer.renderOutline(
            playerModel(), Player.OUTLINE_THICKNESS, this.anim, instances,
            renderedMeshes = listOf("body", "hair")
        )
        client.renderer.renderGeometry(
            playerModel(), this.anim, instances
        )
    }

}