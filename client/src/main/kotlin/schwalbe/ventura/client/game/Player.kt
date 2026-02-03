
package schwalbe.ventura.client.game

import schwalbe.ventura.client.*
import schwalbe.ventura.engine.*
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.net.*
import schwalbe.ventura.net.PacketType.*
import kotlin.math.*
import org.joml.*

object PlayerAnim : Animations<PlayerAnim> {
    val idle = anim("idle")
    val walk = anim("walk")
    val squat = anim("squat")
}

fun PlayerAnim.fromSharedAnim(a: SharedPlayerInfo.Animation) = when (a) {
    SharedPlayerInfo.Animation.IDLE     -> PlayerAnim.idle
    SharedPlayerInfo.Animation.WALK     -> PlayerAnim.walk
    SharedPlayerInfo.Animation.SQUAT    -> PlayerAnim.squat
}

fun AnimationRef<PlayerAnim>.toSharedAnim() = when (this) {
    PlayerAnim.idle     -> SharedPlayerInfo.Animation.IDLE
    PlayerAnim.walk     -> SharedPlayerInfo.Animation.WALK
    PlayerAnim.squat    -> SharedPlayerInfo.Animation.SQUAT
    else -> SharedPlayerInfo.Animation.IDLE
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

        const val PACKET_SEND_INTERVAL_MS: Long = 1000L / 20L

        val relCollider: AxisAlignedBox = axisBoxOf(
            Vector3f(-0.125f, 0f, -0.125f),
            Vector3f(+0.125f, 1f, +0.125f)
        )
    }

    private var wasMoving: Boolean = false
    val anim = AnimState(PlayerAnim.idle)

    val position: Vector3f = Vector3f()
    var rotation: Float = 0f

    private fun move(world: World, client: Client): Boolean {
        val collidingBefore: Boolean = world.chunks
            .intersectsAnyLoaded(Player.relCollider.translate(this.position))
        val velocity = Vector3f()
        if (Key.W.isPressed) { velocity.z -= 1f; }
        if (Key.S.isPressed) { velocity.z += 1f; }
        if (Key.A.isPressed) { velocity.x -= 1f; }
        if (Key.D.isPressed) { velocity.x += 1f; }
        if (velocity.length() == 0f) { return false }
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
        return true
    }

    private fun updateAnimations(moving: Boolean, deltaTime: Float) {
        if (moving != this.wasMoving) {
            this.anim.transitionTo(
                if (moving) { PlayerAnim.walk } else { PlayerAnim.idle },
                0.25f
            )
            this.wasMoving = moving
        }
        this.anim.addTimePassed(deltaTime)
        this.anim.addTransitionTimePassed(
            deltaTime * this.anim.numQueuedTransitions
        )
    }

    var nextPacketSendTime: Long = 0L

    private fun sendPositionUpdatePacket(client: Client) {
        val now: Long = System.currentTimeMillis()
        if (now < this.nextPacketSendTime) { return }
        this.nextPacketSendTime = now + Player.PACKET_SEND_INTERVAL_MS
        client.network.outPackets?.send(Packet.serialize(
            PacketType.PLAYER_STATE,
            SharedPlayerInfo(
                this.position.toSerVector3(),
                this.rotation,
                this.anim.latestAnim.toSharedAnim()
            )
        ))
    }

    fun update(client: Client, captureInput: Boolean) {
        val world: World = client.world ?: return
        val moving: Boolean = if (!captureInput) { false }
            else { this.move(world, client) }
        this.updateAnimations(moving, client.deltaTime)
        this.sendPositionUpdatePacket(client)
    }

    fun render(client: Client) {
        Player.render(client, this.position, this.rotation, this.anim)
    }

}

fun Player.Companion.render(
    client: Client, pos: Vector3fc, rot: Float, anim: AnimState<PlayerAnim>
) {
    val transf = Matrix4f()
        .translate(pos)
        .rotateY(rot)
        .scale(Player.MODEL_SCALE)
    val instances = listOf(transf)
    client.renderer.renderOutline(
        playerModel(), Player.OUTLINE_THICKNESS, anim, instances,
        renderedMeshes = listOf("body", "hair")
    )
    client.renderer.renderGeometry(
        playerModel(), anim, instances
    )
}