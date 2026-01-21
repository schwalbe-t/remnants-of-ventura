
package schwalbe.ventura.bigton

import schwalbe.ventura.bigton.runtime.*
import kotlin.math.*

data class BuiltinFunctionInfo(
    val name: String, val cost: Int, val argc: Int,
    val impl: (BigtonRuntime) -> Unit
)

class BigtonBuiltinFunctions {
    
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

private fun assertSliceIndices(
    length: Int, start: Int, end: Int, r: BigtonRuntime
): Boolean {
    if (start !in 0..length) {
        runtimeError(r,
            "'slice' received start index $start, which is out of bounds for " +
            "length $length ([0 <= startIdx <= endIdx <= len] is required)"
        )
        return false
    }
    if (end !in 0..length) {
        runtimeError(r,
            "'slice' received end index $end, which is out of bounds for " +
            "length $length ([0 <= startIdx <= endIdx <= len] is required)"
        )
        return false
    }
    if (end < start) {
        runtimeError(r,
            "'slice' expects the end index to be larger than or equal to the " +
            "start index, but received a start index $start which is greater " +
            "than the given end index $end " +
            "([0 <= startIdx <= endIdx <= len] is required)"
        )
        return false
    }
    return true
}

private fun slice(r: BigtonRuntime) {
    val endIdx: BigtonValue? = r.popStack()
    val startIdx: BigtonValue? = r.popStack()
    val source: BigtonValue? = r.popStack()
    arrayOf(source, startIdx, endIdx).useAll {
        if (source == null || startIdx == null || endIdx == null) {
            return BigtonNull.create().use(r::pushStack)
        }
        if (startIdx !is BigtonInt || endIdx !is BigtonInt) {
            return runtimeError(r,
                "'slice' expects the start index (second parameter) " +
                "and end index (third parameter) to both be integers, but " +
                "the function received something else"
            )
        }
        val start: Int = startIdx.value.toInt()
        val end: Int = endIdx.value.toInt()
        when {
            source is BigtonString
                -> if (assertSliceIndices(source.length, start, end, r)) {
                source.slice(start, end, r).use(r::pushStack)
            }
            source is BigtonArray
                -> if (assertSliceIndices(source.length, start, end, r)) {
                source.slice(start, end, r).use(r::pushStack)
            }
            else -> return runtimeError(r,
                "'slice' expects the source (first parameter) to be a string " +
                "or an array, but the function received something else"
            )
        }
    }
}

private fun insert(r: BigtonRuntime) {
    val value: BigtonValue? = r.popStack()
    val index: BigtonValue? = r.popStack()
    val dest: BigtonValue? = r.popStack()
    arrayOf(dest, index, value).useAll {
        if (dest == null || index == null || value == null) {
            return BigtonNull.create().use(r::pushStack)
        }
        if (dest !is BigtonArray) { return runtimeError(r,
            "'insert' expects the target container (first argument) " +
            "to be an array, but the function received something else"
        ) }
        if (index !is BigtonInt) { return runtimeError(r,
            "'insert' expects the destination index (second argument) " +
            "to be an integer, but the function received something else"
        ) }
        val i: Int = index.value.toInt()
        if (i !in 0..dest.length) { return runtimeError(r,
            "'insert' expects the destination index (second argument) " +
            "to be in bounds of the given array, but received index $i " +
            "(for an array of length ${dest.length}) " +
            "([0 <= i <= len] is required)"
        ) }
        dest.insert(i, value, r)
        BigtonNull.create().use(r::pushStack)
    }
}

private fun remove(r: BigtonRuntime) {
    val index: BigtonValue? = r.popStack()
    val source: BigtonValue? = r.popStack()
    arrayOf(source, index).useAll {
        if (source == null || index == null) {
            return BigtonNull.create().use(r::pushStack)
        }
        if (source !is BigtonArray) { return runtimeError(r,
            "'remove' expects the source container (first argument) " +
            "to be an array, but the function received something else"
        ) }
        if (index !is BigtonInt) { return runtimeError(r,
            "'remove' expects the source index (second argument) " +
            "to be an integer, but the function received something else"
        ) }
        val i: Int = index.value.toInt()
        if (i !in 0..<source.length) { return runtimeError(r,
            "'remove' expects the source index (second argument) " +
            "to be in bounds of the given array, but received index $i " +
            "(for an array of length ${source.length}) " +
            "([0 <= i < len] is required)"
        ) }
        source.remove(i).use(r::pushStack)
    }
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
                ?.use { displayValue(it, DISPLAY_MAX_DEPTH, r) }
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
        .withFunction("concat", cost = 1, argc = 2) { r ->
            val b: BigtonValue? = r.popStack()
            val a: BigtonValue? = r.popStack()
            arrayOf(a, b).useAll {
                if (a == null || b == null) {
                    return@withFunction BigtonNull.create().use(r::pushStack)
                }
                when {
                    a is BigtonString && b is BigtonString
                        -> BigtonString.concat(a, b, r).use(r::pushStack)
                    a is BigtonArray && b is BigtonArray
                        -> BigtonArray.concat(a, b, r).use(r::pushStack)
                    else -> runtimeError(r,
                        "'concat' expects its arguments to be either both " +
                        "strings or both arrays, but the values the function " +
                        "received do not meet this requirenment"
                    )
                }
            }
        }
        .withFunction("slice", cost = 1, argc = 3, ::slice)
        .withFunction("insert", cost = 1, argc = 3, ::insert)
        .withFunction("push", cost = 1, argc = 2) { r ->
            val value: BigtonValue? = r.popStack()
            val dest: BigtonValue? = r.popStack()
            arrayOf(dest, value).useAll {
                if (dest == null || value == null) {
                    return@withFunction BigtonNull.create().use(r::pushStack)
                }
                if (dest !is BigtonArray) { return@withFunction runtimeError(r,
                    "'push' expects the destination container (first " +
                    "argument) to be an array, but function received " +
                    "something else"
                ) }
                dest.insert(dest.length, value, r)
                BigtonNull.create().use(r::pushStack)
            }
        }
        .withFunction("remove", cost = 1, argc = 2, ::remove)
        .withFunction("pop", cost = 1, argc = 1) { r ->
            val src: BigtonValue? = r.popStack()
                ?: return@withFunction BigtonNull.create().use(r::pushStack)
            src.use {
                if (src !is BigtonArray) { return@withFunction runtimeError(r,
                    "'pop' expects the given source container " +
                    "to be an array, but function received something else"
                ) }
                val srcLen: Int = src.length
                if (srcLen == 0) { return@withFunction runtimeError(r,
                    "'pop' requires the given source container to contain " +
                    "at least one value, but the given array was empty"
                ) }
                src.remove(srcLen - 1).use(r::pushStack)
            }
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