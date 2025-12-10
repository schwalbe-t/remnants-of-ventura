
package schwalbe.ventura.bigton.runtime

import schwalbe.ventura.bigton.*

fun getStandardModule() = BigtonRuntime.Module(
    mapOf(
        "say" to BigtonRuntime.BuiltinFunction(1) { r ->
            r.logs.add(r.popOperand().toString())
        }
        "error" to BigtonRuntime.BuiltinFunction(0) { r ->
            throw BigtonException(
                BigtonErrorType.BY_PROGRAM, r.getCurrentLine()
            )
        }

        // TODO! string operations
        // TODO! min/max/clamp
    )
)

fun getFloatModule() = BigtonRuntime.Module(
    mapOf(
        // TODO! floating point math operations
    )
)

fun getMemoryModule() = BigtonRuntime.Module(
    mapOf(
        // TODO! 'ramSize'
    )
)