
package schwalbe.ventura.server.game.extensions

import kotlinx.serialization.Serializable
import org.joml.Vector3f
import schwalbe.ventura.bigton.BigtonModule
import schwalbe.ventura.bigton.runtime.*
import schwalbe.ventura.data.unitsToUnitIdx
import schwalbe.ventura.server.game.Robot
import kotlin.math.abs

@Serializable
data class LaserAttachmentState(
    var shootCooldown: Int = 0
) : GameAttachment {
    companion object {
        val TYPE = AttachmentStates.register(::LaserAttachmentState)
        const val SHOOT_COOLDOWN_TICKS = 60
        const val MAXIMUM_RANGE = 15
        const val DAMAGE = 25f
    }
    
    val canShoot: Boolean
        get() = this.shootCooldown == 0

    override fun update(ctx: GameAttachmentContext) {
        if (this.shootCooldown > 0) {
            this.shootCooldown -= 1
        }
    }
}

private fun sign(i: Int): Int = when {
    i < 0 -> -1
    i > 0 -> +1
    else -> 0
}

private fun implementLaserShoot(
    r: BigtonRuntime, ctx: GameAttachmentContext, rdx: Int, rdz: Int
) {
    val state = ctx.robot.attachmentStates[LaserAttachmentState.TYPE]
    if (!state.canShoot || (rdx == 0 && rdz == 0)) {
        return BigtonInt.fromValue(0).use(r::pushStack)
    }
    state.shootCooldown = LaserAttachmentState.SHOOT_COOLDOWN_TICKS
    val (dx, dz) = if (abs(rdx) > abs(rdz)) { sign(rdx) to 0 }
        else { 0 to sign(rdz) }
    ctx.robot.rotateWeaponAlong(Vector3f(dx.toFloat(), 0f, dz.toFloat()))
    var cx: Int = ctx.robot.position.x.unitsToUnitIdx()
    var cz: Int = ctx.robot.position.z.unitsToUnitIdx()
    for (i in 1..LaserAttachmentState.MAXIMUM_RANGE) {
        val hitRobot: Robot? = ctx.robot // TODO! determine actual enemy robot to hit
        if (hitRobot != null) {
            hitRobot.health -= LaserAttachmentState.DAMAGE
            return BigtonInt.fromValue(1).use(r::pushStack)
        }
        cx += dx
        cz += dz
    }
    BigtonInt.fromValue(1).use(r::pushStack)
}

val LASER_ATTACHMENT_MODULE = BigtonModule(BIGTON_MODULES.functions)
    .withCtxFunction("laserShootLeft", cost = 1, argc = 0) { r, ctx ->
        implementLaserShoot(r, ctx, -1, 0)
    }
    .withCtxFunction("laserShootRight", cost = 1, argc = 0) { r, ctx ->
        implementLaserShoot(r, ctx, +1, 0)
    }
    .withCtxFunction("laserShootUp", cost = 1, argc = 0) { r, ctx ->
        implementLaserShoot(r, ctx, 0, -1)
    }
    .withCtxFunction("laserShootDown", cost = 1, argc = 0) { r, ctx ->
        implementLaserShoot(r, ctx, 0, +1)
    }
    .withCtxFunction("laserShoot", cost = 1, argc = 1) { r, ctx ->
        val (dx, dz) = r.popTuple2Int()
            ?: return@withCtxFunction r.reportDynError(
                "'laserShoot' expects a tuple of 2 integers, but function " +
                "received something else"
            )
        implementLaserShoot(r, ctx, dx.toInt(), dz.toInt())
    }
    .withCtxFunction("laserCanShoot", cost = 1, argc = 0) { r, ctx ->
        val state = ctx.robot.attachmentStates[LaserAttachmentState.TYPE]
        BigtonInt.fromValue(if (state.canShoot) 1 else 0).use(r::pushStack)
    }
