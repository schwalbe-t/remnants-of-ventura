
package schwalbe.ventura.server.game.extensions

import schwalbe.ventura.bigton.BigtonModule
import schwalbe.ventura.bigton.runtime.*
import schwalbe.ventura.data.unitsToUnitIdx

val GPS_ATTACHMENT_MODULE = BigtonModule(BIGTON_MODULES.functions)
    .withCtxFunction("gpsReceive", cost = 1, argc = 0) { r, ctx ->
        val pos = ctx.robot.position
        val x = BigtonInt.fromValue(pos.x.unitsToUnitIdx().toLong())
        val z = BigtonInt.fromValue(pos.z.unitsToUnitIdx().toLong())
        arrayOf<BigtonValue?>(x, z).useAll {
            BigtonTuple.fromElements(listOf(x, z), r).use(r::pushStack)
        }
    }