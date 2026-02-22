
package schwalbe.ventura.server.game.extensions

import schwalbe.ventura.bigton.BigtonModule
import schwalbe.ventura.bigton.runtime.*
import schwalbe.ventura.data.unitsToUnitIdx
import kotlin.math.hypot

const val SCOUT_MOVEMENT_SPEED: Int = 10 // ticks per unit moved

private fun implementMovement(
    dx: Int, dz: Int
): (BigtonRuntime, GameAttachmentContext) -> Unit = f@{ r, ctx ->
    if (ctx.robot.isMoving) {
        return@f BigtonInt.fromValue(0).use(r::pushStack)
    }
    val len: Float = hypot(dx.toFloat(), dz.toFloat())
    val ndx: Float = dx / len
    val ndz: Float = dz / len
    val destTx: Int = (ctx.robot.position.x + ndx).unitsToUnitIdx()
    val destTz: Int = (ctx.robot.position.z + ndz).unitsToUnitIdx()
    val isOccupied: Boolean = ctx.world.data.chunkCollisions[destTx, destTz]
    if (isOccupied) {
        return@f BigtonInt.fromValue(0).use(r::pushStack)
    }
    ctx.robot.move(ndx, ndz, SCOUT_MOVEMENT_SPEED)
    BigtonInt.fromValue(1).use(r::pushStack)
}

val SCOUT_ROBOT_MODULE = BigtonModule(BIGTON_MODULES.functions)
    .withCtxFunction("moveLeft", cost = 1, argc = 0, implementMovement(-1, 0))
    .withCtxFunction("moveRight", cost = 1, argc = 0, implementMovement(+1, 0))
    .withCtxFunction("moveUp", cost = 1, argc = 0, implementMovement(0, -1))
    .withCtxFunction("moveDown", cost = 1, argc = 0, implementMovement(0, +1))
    .withCtxFunction("canMove", cost = 1, argc = 0) { r, ctx ->
        BigtonInt.fromValue(if (ctx.robot.isMoving) 0 else 1).use(r::pushStack)
    }
