
package schwalbe.ventura.server.game.extensions

import schwalbe.ventura.bigton.runtime.*

fun BigtonRuntime.reportDynError(reason: String) {
    this.logLine("ERROR: $reason")
    this.error = BigtonRuntimeError.BY_PROGRAM
}

fun BigtonRuntime.popTuple2Int(): Pair<Long, Long>? = this.popStack()?.use {
    if (it !is BigtonTuple || it.length != 2) { return@use null }
    val a: BigtonValue = it[0]
    val b: BigtonValue = it[1]
    arrayOf<BigtonValue?>(a, b).useAll {
        if (a !is BigtonInt || b !is BigtonInt) { return@use null }
        a.value to b.value
    }
}
