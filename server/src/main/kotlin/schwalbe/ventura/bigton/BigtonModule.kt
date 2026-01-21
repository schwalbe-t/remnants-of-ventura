
package schwalbe.ventura.bigton

import schwalbe.ventura.bigton.runtime.*
import kotlin.math.*

data class BuiltinFunctionInfo(
    val name: String, val cost: Int, val argc: Int,
    val impl: (BigtonRuntime) -> Unit
)

class BigtonBuiltinFunctions {
    
    private var nextFunId: Int = 0
    
    private val mutFunctionsById: MutableList<BuiltinFunctionInfo>
        = mutableListOf()
    private val mutIdByFunctionName: MutableMap<String, Int>
        = mutableMapOf()
    val functions: List<BuiltinFunctionInfo> = this.mutFunctionsById
    val functionIds: Map<String, Int> = this.mutIdByFunctionName
    
    fun register(info: BuiltinFunctionInfo) {
        val id: Int = this.mutFunctionsById.size
        this.mutFunctionsById.add(info)
        this.mutIdByFunctionName[info.name] = id
    }
    
}

class BigtonModule(val funcOut: BigtonBuiltinFunctions) {
    
    private val mutFunctions: MutableMap<String, BuiltinFunctionInfo>
        = mutableMapOf()
    val functions: Map<String, BuiltinFunctionInfo> = this.mutFunctions
    
    fun withFunction(
        name: String, cost: Int, argc: Int, f: (BigtonRuntime) -> Unit
    ): BigtonModule {
        val info = BuiltinFunctionInfo(name, cost, argc, f)
        this.mutFunctions[name] = info
        this.funcOut.register(info)
        return this
    }

}

const val PRINT_MAX_DEPTH: Int = 5
const val DISPLAY_MAX_DEPTH: Int = 3

private fun displayValue(
    value: BigtonValue, maxDepth: Int, r: BigtonRuntime
): String {
    val nextDepth: Int = maxDepth - 1
    return when (value) {
        is BigtonNull -> "null"
        is BigtonInt -> value.value.toString()
        is BigtonFloat -> value.value.toString()
        is BigtonString -> value.value
        is BigtonTuple -> {
            if (value.length == 0) { return "()" }
            if (maxDepth == 0) { return "(...)" }
            val c: String = (0..<value.length)
                .map { i -> value[i].use { displayValue(it, nextDepth, r) } }
                .joinToString(", ")
            "($c)"
        }
        is BigtonObject -> {
            if (value.size == 0) { return "{}" }
            if (maxDepth == 0) { return "{...}" }
            val c: String = value.properties
                .map { p -> value[p].use { mv ->
                    val propName: String = value.nameOf(p, r)
                    val dispStr: String = displayValue(mv, nextDepth, r)
                    "$propName=$dispStr"
                } }
                .joinToString(", ")
            "{$c}"
        }
        is BigtonArray -> {
            if (value.length == 0) { return "[]" }
            if (maxDepth == 0) { return "[...]" }
            val c: String = (0..<value.length)
                .map { i -> value[i].use { displayValue(it, nextDepth, r) } }
                .joinToString(", ")
            "[$c]"
        }
    }
}

private fun printValue(r: BigtonRuntime) {
    val dispStr: String = r.popStack()
        ?.let { v -> v.use {
            displayValue(it, PRINT_MAX_DEPTH, r)
        } }
        ?: "<empty stack>"
    r.logLine(dispStr)
    BigtonNull.create().use(r::pushStack)
}

private fun runtimeError(r: BigtonRuntime, reason: String) {
    r.logLine(reason)
    r.error = BigtonRuntimeError.BY_PROGRAM
}

private fun parseInt(r: BigtonRuntime) {
    val parsed: Long = r.popStack()
        ?.use { when (it) {
            is BigtonInt -> it.value
            is BigtonFloat -> it.value.toLong()
            is BigtonString -> {
                val contained: String = it.value
                try {
                    contained.toLong()
                } catch (e: NumberFormatException) {
                    return runtimeError(r,
                        "Strings passed to 'int' must represent integers, " +
                        "but function received string '$contained'"
                    )
                }
            }
            else -> return runtimeError(r,
                "'int' expects an integer, float or string, but function " +
                "received something else"
            )
        } }
        ?: 0
    BigtonInt.fromValue(parsed).use(r::pushStack)
}

private fun parseFloat(r: BigtonRuntime) {
    val parsed: Double = r.popStack()
        ?.use { when (it) {
            is BigtonInt -> it.value.toDouble()
            is BigtonFloat -> it.value
            is BigtonString -> {
                val contained: String = it.value
                try {
                    contained.toDouble()
                } catch (e: NumberFormatException) {
                    return runtimeError(r,
                        "Strings passed to 'float' must represent floats, " +
                                "but function received string '$contained'"
                    )
                }
            }
            else -> return runtimeError(r,
                "'float' expects an integer, float or string, but function " +
                "received something else"
            )
        } }
        ?: 0.0
    BigtonFloat.fromValue(parsed).use(r::pushStack)
}

private inline fun wrapFloatFunction(
    name: String, crossinline f: (Double) -> Double
): (BigtonRuntime) -> Unit = impl@{ r ->
    val inp: Double = r.popStack()
        ?.use { when (it) {
            is BigtonFloat -> it.value
            else -> return@impl runtimeError(r,
                "'$name' expects a float, but function received something else"
            )
        } }
        ?: 0.0
    BigtonFloat.fromValue(f(inp)).use(r::pushStack)
}

object BigtonModules {
    
    val functions = BigtonBuiltinFunctions()
    
    val standard = BigtonModule(functions)
        .withFunction("print", cost = 1, argc = 1, ::printValue)
        .withFunction("error", cost = 1, argc = 1) { r ->
            printValue(r)
            r.error = BigtonRuntimeError.BY_PROGRAM
        }
        .withFunction("string", cost = 1, argc = 1) { r ->
            val dispStr: String = r.popStack()
                ?.use { displayValue(it, PRINT_MAX_DEPTH, r) }
                ?: "<empty stack>"
            BigtonString.fromValue(dispStr, r).use(r::pushStack)
        }
        .withFunction("int", cost = 1, argc = 1, ::parseInt)
        .withFunction("len", cost = 1, argc = 1) { r ->
            val length: Int = r.popStack()
                ?.use { when (it) {
                    is BigtonString -> it.length
                    is BigtonTuple -> it.length
                    is BigtonObject -> it.size
                    is BigtonArray -> it.length
                    else -> return@withFunction runtimeError(r,
                        "'len' expects a string, tuple, object or array, " +
                        "but function received something else"
                    )
                } }
                ?: 0
            BigtonInt.fromValue(length.toLong()).use(r::pushStack)
        }
        .withFunction("flatLen", cost = 1, argc = 1) { r ->
            val flatLength: Int = r.popStack()
                ?.use { when (it) {
                    is BigtonTuple -> it.flatLength
                    else -> return@withFunction runtimeError(r,
                        "'flatLen' expects a tuple, but function received " +
                        "something else"
                    )
                } }
                ?: 0
            BigtonInt.fromValue(flatLength.toLong()).use(r::pushStack)
        }

    val floatingPoint = BigtonModule(functions)
        .withFunction("float", cost = 1, argc = 1, ::parseFloat)
        .withFunction("ceil", cost = 1, argc = 1,
            wrapFloatFunction("ceil", ::ceil)
        )
        .withFunction("round", cost = 1, argc = 1,
            wrapFloatFunction("round", ::round)
        )
        .withFunction("floor", cost = 1, argc = 1,
            wrapFloatFunction("floor", ::floor)
        )
        .withFunction("sqrt", cost = 1, argc = 1,
            wrapFloatFunction("sqrt", ::sqrt)
        )
        .withFunction("cbrt", cost = 1, argc = 1,
            wrapFloatFunction("cbrt", ::cbrt)
        )
        .withFunction("sin", cost = 1, argc = 1,
            wrapFloatFunction("sin", ::sin)
        )
        .withFunction("cos", cost = 1, argc = 1,
            wrapFloatFunction("cos", ::cos)
        )
        .withFunction("tan", cost = 1, argc = 1,
            wrapFloatFunction("tan", ::tan)
        )
}