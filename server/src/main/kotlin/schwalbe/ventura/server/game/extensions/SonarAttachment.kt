
package schwalbe.ventura.server.game.extensions

import schwalbe.ventura.bigton.BigtonModule
import schwalbe.ventura.bigton.runtime.*

private fun sonarLocateImpl(
    dx: Int, dz: Int, maxDistTiles: Int
): (BigtonRuntime, GameAttachmentContext) -> Unit = f@{ r, ctx ->
    var currTx: Int = ctx.robot.tileX
    var currTz: Int = ctx.robot.tileZ
    var numSteps = 0
    while (numSteps < maxDistTiles) {
        currTx += dx
        currTz += dz
        if (ctx.world.tileIsOccupied(currTx, currTz)) {
            return@f BigtonInt.fromValue(numSteps.toLong()).use(r::pushStack)
        }
        numSteps += 1
    }
    return@f BigtonNull.create().use(r::pushStack)
}

fun makeSonarAttachmentModule(
    maxDistTiles: Int
) = BigtonModule(BIGTON_MODULES.functions)
    .withCtxFunction(
        "sonarDistLeft", cost = 1, argc = 0,
        sonarLocateImpl(-1, 0, maxDistTiles)
    )
    .withCtxFunction(
        "sonarDistRight", cost = 1, argc = 0,
        sonarLocateImpl(+1, 0, maxDistTiles)
    )
    .withCtxFunction(
        "sonarDistUp", cost = 1, argc = 0,
        sonarLocateImpl(0, -1, maxDistTiles)
    )
    .withCtxFunction(
        "sonarDistDown", cost = 1, argc = 0,
        sonarLocateImpl(0, +1, maxDistTiles)
    )
