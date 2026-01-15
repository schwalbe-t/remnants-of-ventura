
package schwalbe.ventura.bigton

import schwalbe.ventura.bigton.runtime.*

class BigtonBuiltinFunctions {
    
    data class FunctionInfo(
        val name: String, val cost: Int,
        val impl: (Long) -> Unit
    )
    
    
    private var nextFunId: Int = 0
    
    private val mutFunctionsById: MutableList<FunctionInfo>
        = mutableListOf()
    val functionImpls: List<FunctionInfo> = this.mutFunctionsById
        
    fun register(name: String, cost: Int, f: (Long) -> Unit) {
        this.mutFunctionsById.add(FunctionInfo(name, cost, f))
    }
    
}

class BigtonModule(val funcOut: BigtonBuiltinFunctions) {
    
    private val mutFunctions: MutableSet<String> = mutableSetOf()
    val functions: Set<String> = this.mutFunctions
    
    fun withFunction(name: String, cost: Int, f: (Long) -> Unit) {
        this.mutFunctions.add(name)
        this.funcOut.register(name, cost, f)
    }

}

private fun displayValue(value: Long, maxDepth: Int): String {
    if (maxDepth == 0) { return "..." }
    val nextDepth: Int = maxDepth - 1
    return when (BigtonValue.getType(value)) {
        BigtonValue.Type.NULL -> "null"
        BigtonValue.Type.INT -> BigtonValue.getInt(value).toString()
        BigtonValue.Type.FLOAT -> BigtonValue.getFloat(value).toString()
        BigtonValue.Type.TUPLE -> {
            val length: Int = BigtonValue.getTupleLength(value)
            val contents: String = (0..<length)
                .joinToString(", ", transform = { i ->
                    val mv: Long = BigtonValue.getTupleMember(value, i)
                    val str: String = displayValue(mv, nextDepth)
                    BigtonValue.free(mv)
                    str
                })
            "($contents)"
        }
        BigtonValue.Type.OBJECT -> {
            error("TODO!")
        }
        BigtonValue.Type.ARRAY -> {
            error("TODO!")
        }
        else -> "<unknown>"
    }
}

const val PRINT_MAX_DEPTH: Int = 3

object BigtonModules {
    
    val functions = BigtonBuiltinFunctions()
    
    val standard = BigtonModule(functions)
        .withFunction("print", cost = 1) { r ->
            val value = BigtonRuntime.popStack(r)
            check(value != 0L)
            val valStr: String = displayValue(value, PRINT_MAX_DEPTH)
            val line: Long = BigtonValue.createString(valStr, r)
            BigtonRuntime.addLogLine(r, line)
            BigtonValue.free(line)
            BigtonValue.free(value)
        }
    
}