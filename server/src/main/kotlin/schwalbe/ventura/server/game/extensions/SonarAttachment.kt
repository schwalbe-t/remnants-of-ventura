
package schwalbe.ventura.server.game.extensions

import schwalbe.ventura.bigton.BigtonModule
import schwalbe.ventura.bigton.runtime.*
import schwalbe.ventura.utils.sign
import kotlin.math.abs

private fun implementSonarLocate(
    r: BigtonRuntime, ctx: GameAttachmentContext, rdx: Int, rdz: Int,
    maxDistTiles: Int
) {
    if (rdx == 0 && rdz == 0) {
        return BigtonNull.create().use(r::pushStack)
    }
    val (dx, dz) = if (abs(rdx) > abs(rdz)) { sign(rdx) to 0 }
        else { 0 to sign(rdz) }
    var currTx: Int = ctx.robot.tileX
    var currTz: Int = ctx.robot.tileZ
    var numSteps = 0
    while (numSteps < maxDistTiles) {
        currTx += dx
        currTz += dz
        if (ctx.world.tileIsOccupied(currTx, currTz)) {
            return BigtonInt.fromValue(numSteps.toLong()).use(r::pushStack)
        }
        numSteps += 1
    }
    return BigtonInt.fromValue(maxDistTiles.toLong()).use(r::pushStack)
}

fun makeSonarAttachmentModule(
    maxDistTiles: Int
) = BigtonModule(BIGTON_MODULES.functions)
    .withCtxFunction("sonarDist", cost = 1, argc = 1) { r, ctx ->
        val (dx, dz) = r.popTuple2Int()
            ?: return@withCtxFunction r.reportDynError(
                "'sonarDist' expects a tuple of 2 integers, but function " +
                "received something else"
            )
        implementSonarLocate(r, ctx, dx.toInt(), dz.toInt(), maxDistTiles)
    }
