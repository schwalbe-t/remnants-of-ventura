
package schwalbe.ventura.server.game.extensions

import schwalbe.ventura.bigton.runtime.*
import schwalbe.ventura.utils.sign
import kotlin.math.abs

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

fun tileDirToCardinal(rdx: Long, rdz: Long): Pair<Long, Long>
    = if (abs(rdx) >= abs(rdz)) sign(rdx) to 0L else 0L to sign(rdz)

fun tileDirToLesserCardinal(rdx: Long, rdz: Long): Pair<Long, Long>
    = if (abs(rdx) < abs(rdz)) sign(rdx) to 0L else 0L to sign(rdz)

