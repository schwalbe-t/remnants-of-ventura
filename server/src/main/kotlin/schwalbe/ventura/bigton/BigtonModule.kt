
package schwalbe.ventura.bigton

import schwalbe.ventura.bigton.runtime.*

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
            error("TODO!")
            //     BigtonValue.Type.OBJECT -> {
            //         val size: Int = BigtonValue.getObjectSize(value)
            //         if (size == 0) { return "{}" }
            //         if (maxDepth == 0) { return "{...}" }
            //         val c: String = (0..<size).joinToString(", ", transform = { i ->
            //             val nid: Int = BigtonValue.getObjectPropName(value, i, r)
            //             val name: String = BigtonRuntime.getConstString(r, nid)
            //             val mv: Long = BigtonValue.getObjectMember(value, i)
            //             val mvStr: String = displayValue(mv, nextDepth, r)
            //             BigtonValue.free(mv)
            //             "$name=$mvStr"
            //         })
            //         "{$c}"
            //     }
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

const val PRINT_MAX_DEPTH: Int = 5
const val DISPLAY_MAX_DEPTH: Int = 3

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
                ?.let { v -> v.use {
                    displayValue(it, PRINT_MAX_DEPTH, r)
                } }
                ?: "<empty stack>"
            BigtonString.fromValue(dispStr, r).use(r::pushStack)
        }
        // TODO! 'int'
        // TODO! 'float'
}