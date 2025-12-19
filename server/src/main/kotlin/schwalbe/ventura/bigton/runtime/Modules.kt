
package schwalbe.ventura.bigton.runtime

import schwalbe.ventura.bigton.*

val bigtonStandardModule = BigtonRuntime.Module(
    mapOf(
        "say" to BigtonRuntime.Function(cost = 1, argc = 1) { r ->
            r.logLine(r.popOperand().display())
            r.pushOperand(BigtonNull)
        },
        "error" to BigtonRuntime.Function(cost = 0, argc = 0) { r ->
            throw BigtonException(
                BigtonErrorType.BY_PROGRAM, r.getCurrentSource()
            )
        },

        "typeOf" to BigtonRuntime.Function(cost = 1, argc = 1) { r ->
            r.pushOperand(BigtonString(when (r.popOperand()) {
                is BigtonNull -> "null"
                is BigtonInt -> "int"
                is BigtonFloat -> "float"
                is BigtonString -> "string"
                is BigtonTuple -> "tuple"
                is BigtonObject -> "object"
            }))
        },
        "toString" to BigtonRuntime.Function(cost = 1, argc = 1) { r ->
            r.pushOperand(BigtonString(r.popOperand().display()))
        }

        // TODO! string operations
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