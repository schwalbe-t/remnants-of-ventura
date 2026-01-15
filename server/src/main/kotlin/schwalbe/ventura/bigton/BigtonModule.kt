
package schwalbe.ventura.bigton

import schwalbe.ventura.bigton.runtime.*

data class BuiltinFunctionInfo(
    val name: String, val cost: Int, val argc: Int,
    val impl: (Long) -> Unit
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
        name: String, cost: Int, argc: Int, f: (Long) -> Unit
    ): BigtonModule {
        val info = BuiltinFunctionInfo(name, cost, argc, f)
        this.mutFunctions[name] = info
        this.funcOut.register(info)
        return this
    }

}

private fun displayValue(value: Long, maxDepth: Int, r: Long): String {
    val nextDepth: Int = maxDepth - 1
    return when (BigtonValue.getType(value)) {
        BigtonValue.Type.NULL -> "null"
        BigtonValue.Type.INT -> BigtonValue.getInt(value).toString()
        BigtonValue.Type.FLOAT -> BigtonValue.getFloat(value).toString()
        BigtonValue.Type.STRING -> BigtonValue.getString(value)
        BigtonValue.Type.TUPLE -> {
            val length: Int = BigtonValue.getTupleLength(value)
            if (length == 0) { return "()" }
            if (maxDepth == 0) { return "(...)" }
            val c: String = (0..<length).joinToString(", ", transform = { i ->
                val mv: Long = BigtonValue.getTupleMember(value, i)
                val str: String = displayValue(mv, nextDepth, r)
                BigtonValue.free(mv)
                str
            })
            "($c)"
        }
        BigtonValue.Type.OBJECT -> {
            val size: Int = BigtonValue.getObjectSize(value)
            if (size == 0) { return "{}" }
            if (maxDepth == 0) { return "{...}" }
            val c: String = (0..<size).joinToString(", ", transform = { i ->
                val nid: Int = BigtonValue.getObjectPropName(value, i, r)
                val name: String = BigtonRuntime.getConstString(r, nid)
                val mv: Long = BigtonValue.getObjectMember(value, i)
                val mvStr: String = displayValue(mv, nextDepth, r)
                BigtonValue.free(mv)
                "$name=$mvStr"
            })
            "{$c}"
        }
        BigtonValue.Type.ARRAY -> {
            val length: Int = BigtonValue.getArrayLength(value)
            if (maxDepth == 0) {
                return if (length == 0) { "[]" } else { "[...]" }
            }
            val c: String = (0..<length).joinToString(", ", transform = { i ->
                val ev: Long = BigtonValue.getArrayElement(value, i)
                val str: String = displayValue(ev, nextDepth, r)
                BigtonValue.free(ev)
                str
            })
            "[$c]"
        }
        else -> "<unknown>"
    }
}

const val PRINT_MAX_DEPTH: Int = 5
const val DISPLAY_MAX_DEPTH: Int = 3

object BigtonModules {
    
    val functions = BigtonBuiltinFunctions()
    
    val standard = BigtonModule(functions)
        .withFunction("print", cost = 1, argc = 1) { r ->
            val value = BigtonRuntime.popStack(r)
            val disp: Long = if (value != 0L) {
                val dispStr: String = displayValue(value, PRINT_MAX_DEPTH, r)
                BigtonValue.free(value)
                BigtonValue.createString(dispStr, r)
            } else {
                BigtonValue.createString("<empty stack>", r)
            }
            BigtonRuntime.addLogLine(r, disp)
            BigtonValue.free(disp)
        }
        .withFunction("string", cost = 1, argc = 1) { r ->
            val value = BigtonRuntime.popStack(r)
            val disp: Long = if (value != 0L) {
                val dispStr: String = displayValue(value, DISPLAY_MAX_DEPTH, r)
                BigtonValue.free(value)
                BigtonValue.createString(dispStr, r)
            } else {
                BigtonValue.createString("<empty stack>", r)
            }
            BigtonRuntime.pushStack(r, disp)
            BigtonValue.free(disp)
        }
        // TODO! 'int'
        // TODO! 'float'
}