
package schwalbe.ventura.bigton.runtime

import schwalbe.ventura.bigton.*

val bigtonStandardModule = BigtonRuntime.Module(
    mapOf(
        "print" to BigtonRuntime.Function(cost = 1, argc = 1) { r ->
            r.logLine(r.popOperand().display())
            r.pushOperand(BigtonNull)
        },
        "error" to BigtonRuntime.Function(cost = 0, argc = 1) { r ->
            r.logLine(r.popOperand().display())
            throw BigtonException(
                BigtonErrorType.BY_PROGRAM, r.getCurrentSource()
            )
        },

        "typeOf" to BigtonRuntime.Function(cost = 1, argc = 1) { r ->
            r.pushOperand(BigtonString(r.popOperand().typeString()))
        },
        "toString" to BigtonRuntime.Function(cost = 1, argc = 1) { r ->
            r.pushOperand(BigtonString(r.popOperand().display()))
        },
        
        "len" to BigtonRuntime.Function(cost = 1, argc = 1) { r ->
            val value: BigtonValue = r.popOperand()
            r.pushOperand(BigtonInt(when (value) {
                is BigtonString -> value.v.length.toLong()
                is BigtonTuple -> value.elements.size.toLong()
                // TODO! add arrays
                else -> {
                    val ts: String = value.typeString()
                    r.logLine(
                        "'len' received a value of type '$ts', which does not" +
                            " have a length"
                    )
                    throw BigtonException(
                        BigtonErrorType.BY_PROGRAM, r.getCurrentSource()
                    )
                }
            }))
        },
        "flatLen" to BigtonRuntime.Function(cost = 1, argc = 1) { r ->
            val value: BigtonValue = r.popOperand()
            if (value is BigtonTuple) {
                r.pushOperand(BigtonInt(value.flatLen.toLong()))
                return@Function
            }
            val ts: String = value.typeString()
            r.logLine(
                "'flatLen' expects a tuple but received a value of type '$ts'"
            )
            throw BigtonException(
                BigtonErrorType.BY_PROGRAM, r.getCurrentSource()
            )
        }

        // TODO! string operations
        // TODO! array operations
        // TODO! min/max/clamp
    )
)

val bigtonFloatModule = BigtonRuntime.Module(
    mapOf(
        // TODO! floating point math operations
    )
)

val bigtonMemoryModule = BigtonRuntime.Module(
    mapOf(
        // TODO! 'ramSize'
    )
)