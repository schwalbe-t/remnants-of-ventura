
package schwalbe.ventura.client.game

import schwalbe.ventura.client.*
import schwalbe.ventura.engine.*
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.net.*
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

private fun rotateTowardsPoint(
    baseDir: Vector3fc, targetPoint: Vector3fc, axisFactors: Vector3fc,
    localToWorld: Matrix4fc, weight: () -> Float
): (Matrix4f, Matrix4fc) -> Matrix4f = { jointToParent, parentToLocal ->
    val jointToWorld: Matrix4f = Matrix4f(localToWorld)
        .mul(parentToLocal)
        .mul(jointToParent)
    val headPosWorld = jointToWorld.getTranslation(Vector3f(0f, 0f, 0f))
    val toTargetWorld = Vector3f(targetPoint).sub(headPosWorld).normalize()
    val headRotInv = jointToWorld.get3x3(Matrix3f()).invert()
    val toTargetLocal = headRotInv.transform(toTargetWorld).normalize()
    val forwardWorld = localToWorld
        .transformDirection(Vector3f(baseDir))
        .normalize()
    val forwardLocal = headRotInv
        .transform(Vector3f(forwardWorld))
        .normalize()
    val deltaRot = Quaternionf().rotationTo(forwardLocal, toTargetLocal)
    val deltaRotWeighted = Quaternionf().identity().slerp(deltaRot, weight())
    val deltaRotEuler = deltaRotWeighted
        .getEulerAnglesXYZ(Vector3f()).mul(axisFactors)
    jointToParent.rotateXYZ(deltaRotEuler)
}

class Player {

    companion object {
        fun submitResources(loader: ResourceLoader) = loader.submitAll(
            playerModel
        )

        const val MODEL_SCALE: Float = 1/5.5f
        val MODEL_NO_ROTATION_DIR: Vector3fc = Vector3f(0f, 0f, +1f)

        const val WALK_SPEED: Float = 3f
        const val ROTATION_SPEED: Float = 2f * PI.toFloat() * 1.5f // radians per second

        const val OUTLINE_THICKNESS: Float = 0.015f

        const val PACKET_SEND_INTERVAL_MS: Long = 1000L / 20L

        val relCollider: AxisAlignedBox = axisBoxOf(
            Vector3f(-0.125f, 0f, -0.125f),
            Vector3f(+0.125f, 1f, +0.125f)
        )
    }

    val anim = AnimState(PlayerAnim.idle)

    val position: Vector3f = Vector3f()
    var rotation: SmoothedFloat = 0f
        .smoothed(response = 10f, epsilon = 0.001f)
    var animInjectWeight: SmoothedFloat = 0f
        .smoothed(response = 10f, epsilon = 0.01f)

    fun rotateAlong(direction: Vector3fc) {
        var targetRot = xzVectorAngle(
            Player.MODEL_NO_ROTATION_DIR, Vector3f(direction).normalize()
        )
        this.rotation.target += wrapAngle(targetRot - this.rotation.target)
    }

    fun stopRotation() {
        this.rotation.target = this.rotation.value
    }

    fun assertAnimation(
        targetAnim: AnimationRef<PlayerAnim>, transitionSecs: Float = 0.25f
    ) {
        if (this.anim.latestAnim == targetAnim) { return }
        this.anim.transitionTo(targetAnim, transitionSecs)
    }

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
        this.rotateAlong(velocity)
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
        return true
    }

    private fun updateAnimations(moving: Boolean?, deltaTime: Float) {
        if (moving != true) {
            this.stopRotation()
        }
        if (moving != null) {
            val targetAnim = if (moving) { PlayerAnim.walk }
                else { PlayerAnim.idle }
            this.assertAnimation(targetAnim)
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
                this.rotation.value,
                this.anim.latestAnim.toSharedAnim()
            )
        ))
    }

    fun update(client: Client, captureInput: Boolean) {
        val world: World = client.world ?: return
        val moving: Boolean? = if (!captureInput) { null }
            else { this.move(world, client) }
        this.animInjectWeight.target = 0f
        if (this.animInjectWeight.value == 0f) {
            this.anim.injections.clear()
        }
        this.updateAnimations(moving, client.deltaTime)
        this.sendPositionUpdatePacket(client)
    }

    fun facePoint(target: Vector3fc) {
        this.animInjectWeight.target = 1f
        val localToWorld: Matrix4f = Player
            .modelTransform(this.position, this.rotation.value)
        fun rotateTowardsTarget(
            all: Float, x: Float, y: Float, z: Float
        ) = rotateTowardsPoint(
            MODEL_NO_ROTATION_DIR, target, Vector3f(x, y, z), localToWorld,
            weight = { all * this.animInjectWeight.value }
        )
        this.anim.injections["head"] =
            rotateTowardsTarget(all = 0.33f, x = 0.75f, y = 1f, z = 0.50f)
        this.anim.injections["neck"] =
            rotateTowardsTarget(all = 0.33f, x = 0.50f, y = 1f, z = 0.25f)
        this.anim.injections["body_upper"] =
            rotateTowardsTarget(all = 0.33f, x = 0.25f, y = 1f, z = 0.00f)
        this.anim.injections["shoulder_left"] =
            rotateTowardsTarget(all = 0.2f, x = 0.00f, y = 1f, z = 0.00f)
        this.anim.injections["shoulder_right"] =
            rotateTowardsTarget(all = 0.2f, x = 0.00f, y = 1f, z = 0.00f)
    }

    fun render(pass: RenderPass) {
        this.animInjectWeight.update()
        this.rotation.update()
        Player.render(pass, this.position, this.rotation.value, this.anim)
    }

}

fun Player.Companion.modelTransform(
    pos: Vector3fc, rotY: Float
): Matrix4f = Matrix4f()
    .translate(pos)
    .rotateY(rotY)
    .scale(Player.MODEL_SCALE)

fun Player.Companion.render(
    pass: RenderPass, pos: Vector3fc, rotY: Float, anim: AnimState<PlayerAnim>
) {
    val transf = Player.modelTransform(pos, rotY)
    val instances = listOf(transf)
    pass.renderOutline(
        playerModel(), Player.OUTLINE_THICKNESS, anim, instances,
        renderedMeshes = listOf("body", "hair")
    )
    pass.renderGeometry(
        playerModel(), anim, instances
    )
}