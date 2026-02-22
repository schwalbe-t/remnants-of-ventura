
package schwalbe.ventura.server.game.extensions

import kotlinx.serialization.Serializable
import schwalbe.ventura.bigton.BigtonModule
import schwalbe.ventura.bigton.runtime.*
import schwalbe.ventura.data.unitsToUnitIdx
import kotlin.math.hypot

@Serializable
data class ScoutRobotState(
    var moveCooldownTicks: Int = 0
) : GameAttachment {
    companion object {
        val TYPE = AttachmentStates.register(::ScoutRobotState)
        const val MOVEMENT_COOLDOWN_TICKS: Int = 10
    }

    val canMove: Boolean
        get() = this.moveCooldownTicks == 0

    override fun update(ctx: GameAttachmentContext) {
        if (this.moveCooldownTicks > 0) {
            this.moveCooldownTicks -= 1
        }
    }
}

private fun implementMovement(
    dx: Int, dz: Int
): (BigtonRuntime, GameAttachmentContext) -> Unit = f@{ r, ctx ->
    val state = ctx.robot.attachmentStates[ScoutRobotState.TYPE]
    if (!state.canMove) {
        return@f BigtonInt.fromValue(0).use(r::pushStack)
    }
    val len: Float = hypot(dx.toFloat(), dz.toFloat())
    val ndx: Float = dx / len
    val ndz: Float = dz / len
    val destTx: Int = (ctx.robot.tileX + ndx).unitsToUnitIdx()
    val destTz: Int = (ctx.robot.tileZ + ndz).unitsToUnitIdx()
    val isOccupied: Boolean = ctx.world.data.chunkCollisions[destTx, destTz]
    if (isOccupied) {
        return@f BigtonInt.fromValue(0).use(r::pushStack)
    }
    ctx.robot.tileX = destTx
    ctx.robot.tileZ = destTz
    state.moveCooldownTicks = ScoutRobotState.MOVEMENT_COOLDOWN_TICKS
    BigtonInt.fromValue(1).use(r::pushStack)
}

val SCOUT_ROBOT_MODULE = BigtonModule(BIGTON_MODULES.functions)
    .withCtxFunction("moveLeft", cost = 1, argc = 0, implementMovement(-1, 0))
    .withCtxFunction("moveRight", cost = 1, argc = 0, implementMovement(+1, 0))
    .withCtxFunction("moveUp", cost = 1, argc = 0, implementMovement(0, -1))
    .withCtxFunction("moveDown", cost = 1, argc = 0, implementMovement(0, +1))
    .withCtxFunction("canMove", cost = 1, argc = 0) { r, ctx ->
        val state = ctx.robot.attachmentStates[ScoutRobotState.TYPE]
        BigtonInt.fromValue(if (state.canMove) 1 else 0).use(r::pushStack)
    }
