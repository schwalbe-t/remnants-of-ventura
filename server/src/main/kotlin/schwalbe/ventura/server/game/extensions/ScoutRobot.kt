
package schwalbe.ventura.server.game.extensions

import schwalbe.ventura.bigton.BigtonModule
import schwalbe.ventura.bigton.runtime.*
import schwalbe.ventura.data.unitsToUnitIdx
import kotlin.math.abs
import kotlin.math.sign

const val SCOUT_MOVEMENT_SPEED: Int = 10 // ticks per unit moved

private fun implementMovement(
    r: BigtonRuntime, ctx: GameAttachmentContext, rdx: Int, rdz: Int
) {
    if (ctx.robot.isMoving || (rdx == 0 && rdz == 0)) {
        return BigtonInt.fromValue(0).use(r::pushStack)
    }
    val (dx, dz) = if (abs(rdx) > abs(rdz)) { sign(rdx.toFloat()) to 0f }
        else { 0f to sign(rdz.toFloat()) }
    val destTx: Int = (ctx.robot.position.x + dx).unitsToUnitIdx()
    val destTz: Int = (ctx.robot.position.z + dz).unitsToUnitIdx()
    val isOccupied: Boolean = ctx.world.tileIsOccupied(destTx, destTz)
    if (isOccupied) {
        return BigtonInt.fromValue(0).use(r::pushStack)
    }
    ctx.robot.move(dx, dz, SCOUT_MOVEMENT_SPEED)
    BigtonInt.fromValue(1).use(r::pushStack)
}

val SCOUT_ROBOT_MODULE = BigtonModule(BIGTON_MODULES.functions)
    .withCtxFunction("moveLeft", cost = 1, argc = 0) { r, ctx ->
        implementMovement(r, ctx, -1, 0)
    }
    .withCtxFunction("moveRight", cost = 1, argc = 0) { r, ctx ->
        implementMovement(r, ctx, +1, 0)
    }
    .withCtxFunction("moveUp", cost = 1, argc = 0) { r, ctx ->
        implementMovement(r, ctx, 0, -1)
    }
    .withCtxFunction("moveDown", cost = 1, argc = 0) { r, ctx ->
        implementMovement(r, ctx, 0, +1)
    }
    .withCtxFunction("move", cost = 1, argc = 1) { r, ctx ->
        val (dx, dz) = r.popTuple2Int()
            ?: return@withCtxFunction r.reportDynError(
                "'move' expects a tuple of 2 integers, but function received " +
                "something else"
            )
        implementMovement(r, ctx, dx.toInt(), dz.toInt())
    }
    .withCtxFunction("canMove", cost = 1, argc = 0) { r, ctx ->
        BigtonInt.fromValue(if (ctx.robot.isMoving) 0 else 1).use(r::pushStack)
    }
