
package schwalbe.ventura.server.game.extensions

import schwalbe.ventura.bigton.BigtonModule
import schwalbe.ventura.bigton.runtime.*
import schwalbe.ventura.data.unitsToUnitIdx

const val SCOUT_MOVEMENT_SPEED: Int = 10 // ticks per unit moved

private fun implementMovement(
    r: BigtonRuntime, ctx: GameAttachmentContext, rdx: Long, rdz: Long
) {
    if (ctx.robot.isMoving || (rdx == 0L && rdz == 0L)) {
        return BigtonInt.fromValue(0).use(r::pushStack)
    }
    val (dx, dz) = tileDirToCardinal(rdx, rdz)
    val destTx: Int = (ctx.robot.position.x + dx).unitsToUnitIdx()
    val destTz: Int = (ctx.robot.position.z + dz).unitsToUnitIdx()
    val isOccupied: Boolean = ctx.world.tileIsOccupied(destTx, destTz)
    if (isOccupied) {
        return BigtonInt.fromValue(0).use(r::pushStack)
    }
    ctx.robot.move(dx.toFloat(), dz.toFloat(), SCOUT_MOVEMENT_SPEED)
    BigtonInt.fromValue(1).use(r::pushStack)
}

val SCOUT_ROBOT_MODULE = BigtonModule(BIGTON_MODULES.functions)
    .withCtxFunction("moveAsync", cost = 1, argc = 1) { r, ctx ->
        val (dx, dz) = r.popTuple2Int()
            ?: return@withCtxFunction r.reportDynError(
                "'move' expects a tuple of 2 integers, but function received " +
                "something else"
            )
        implementMovement(r, ctx, dx, dz)
    }
    .withCtxFunction("canMove", cost = 1, argc = 0) { r, ctx ->
        BigtonInt.fromValue(if (ctx.robot.isMoving) 0 else 1).use(r::pushStack)
    }
    .withSrcFile("BUILTIN/scout.bigton", """
        fun move(dir) {
            var startedMoving = moveAsync(dir)
            if not startedMoving { return 0 }
            tick {
                if canMove() { break }
            }
            return 1
        }
    """.trimIndent())
