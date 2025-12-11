
package schwalbe.ventura.bigton.runtime

import schwalbe.ventura.bigton.*

class BigtonRuntime(
    program: BigtonProgram,
    modules: List<BigtonRuntime.Module>,
    memorySize: Int,
    val tickInstructionLimit: Long
) {

    data class Function(
        val instrCost: Long,
        val impl: (BigtonRuntime) -> Unit,
    )

    data class Module(
        val builtinFunctions: Map<String, BuiltinFunction>
    )

    private enum class ScopeType {
        GLOBAL,
        FUNCTION,
        LOOP,
        IF,
        TICK
    }

    private data class Scope(
        val type: ScopeType,
        val body: List<BigtonRuntime>,
        var current: Int = 0
    )

    private data class Call(
        val name: String,
        val fromLine: Int,
        val variables: MutableMap<String, Any?> = mutableListOf()
    )


    private val functions: Map<String, Function>

    init {
        val functions = mutableMapOf<String, Function>()
        for (module in modules) {
            for ((name, f) in module.builtinFunctions) {
                functions[name] = f
            }
        }
        for ((name, body) in program.functions) {
            functions[name] = Function(0) { r ->
                r.calls.add(Call(name, this.currentLine))
                r.scopes.add(Scope(ScopeType.FUNCTION, body))
            }
        }
        this.functions = functions
    }

    val log: MutableList<String> = mutableListOf()

    private val memory: Array<Any?> = arrayOfNulls<Any?>(memorySize)
    private val operands: MutableList<Any?> = mutableListOf()
    private val scopes: MutableList<Scope> = mutableListOf(
        Scope(ScopeType.GLOBAL, program.global)
    )
    private val calls: MutableList<Call> = mutableListOf()
    private val globals: MutableMap<String, Any?> = mutableMapOf()
    private var currentFuncName: String = "<global>"
    private var currentLine: Int = 0

    fun pushOperand(value: Any?) {
        this.operands.add(value)
    }

    fun popOperand(): Any? {
        if (this.operands.size == 0) {
            return this.operands.removeLast()
        }
        throw BigtonException(
            BigtonErrorType.MISSING_OPERAND, this.currentLine
        )
    }

    inline fun<reified T> castPopOperand(): T {
        val v: Any? = this.popOperand()
        if (v is T) { return v }
        throw BigtonException(
            BigtonErrorType.INVALID_TYPE, this.currentLine
        )
    }

    fun addressPopOperand(): Int {
        val address: Long = this.castPopOperand<Long>()
        if (address < 0) {
            throw BigtonException(
                BigtonErrorType.MEMORY_ADDRESS_NEGATIVE, this.currentLine
            )
        } else if (address >= this.memory.size) {
            throw BigtonException(
                BigtonErrorType.MEMORY_ADDRESS_TOO_LARGE, this.currentLine
            )
        }
        return address.toInt()
    }

    fun getCurrentLine(): Int = this.currentLine

    private fun tryGetVarScopeWith(name: String): MutableMap<String, Any?> {
        val locals: MutableMap<String, Any?>?
            = this.calls.lastOrNull()?.variables
        if (locals == null) { return this.globals }
        if (!locals.containsKey(name)) { return this.globals }
        return locals
    }

    private fun forceVarScopeWith(name: String): MutableMap<String, Any?> {
        val possible: MutableMap<String, Any?> = this.tryGetVarScopeWith(name)
        if (possible.containsKey(name)) { return possible }
        throw BigtonException(
            BigtonErrorType.MISSING_VARIABLE, this.currentLine
        )
    }

    private fun executeBinaryNumOp(
        i: (Long, Long) -> Any?, f: (Double, Double) -> Any?
    ) {
        val b: Any? = this.popOperand()
        val a: Any? = this.popOperand()
        if (a is Long && b is Long) {
            this.pushOperand(i(a, b))
        } else if (a is Double && b is Double) {
            this.pushOperand(f(a, b))
        } else {
            throw BigtonException(
                BigtonErrorType.INVALID_TYPE, this.currentLine
            )
        }
    }

    private fun valuesEqual(a: Any?, b: Any?): Boolean {
        if (a == null && b == null) { return true }
        if (a == null || b == null) { return false }
        if (a is Long && b is Long) { return a == b }
        if (a is Double && b is Double) { return a == b }
        if (a is String && b is String) { return a == b }
        if (a is List<Any?> && b is List<Any?>) {
            if (a.size != b.size) { return false }
            for (i in 0..a.size) {
                if (!this.valuesEqual(a[i], b[i])) { return false }
            }
            return true
        }
        if (a is MutableMap<String, Any?> && b is MutalbeMap<String, Any?>) {
            if (a.size != b.size) { return false }
            if (a.keys.any { k -> !b.containsKey(k) }) { return false }
            if (b.keys.any { k -> !a.containsKey(k) }) { return false }
            for (k in a.keys) {
                if (!this.valuesEqual(a[k], b[k])) { return false }
            }
            return true
        }
        throw BigtonException(
            BigtonErrorType.INVALID_TYPE, this.currentLine
        )
    }

    private fun isTruthy(v: Any?): Boolean {
        if (v == null) { return false }
        if (v is Long) { return v != 0L }
        if (v is Double) { return v != 0.0 && !v.isNaN() }
        if (v is String) { return true }
        if (v is List<String, Any?>) { return true }
        if (v is MutableMap<String, Any?>) { return true }
        return false
    }

    fun executeTick() {
        var instructionLimit: Long = 0
        fetchExec@ while (true) {
            val scope: Scope = this.scopes.last()
            val instr: BigtonInstr? = scope.body.getOrNull(scope.current)
            if (instr == null) {
                when (scope.type) {
                    is ScopeType.GLOBAL -> {
                        break@fetchExec
                    }
                    is ScopeType.FUNCTION -> {
                        this.scopes.removeLast()
                        this.calls.removeLast()
                        this.pushOperand(null)
                    }
                    is ScopeType.LOOP -> {
                        scope.current = 0
                    }
                    is ScopeType.IF -> {
                        this.scopes.removeLast()
                    }
                    is ScopeType.TICK -> {
                        scope.current = 0
                        break@fetchExec
                    }
                }
                continue@fetchExec
            }
            if (instructionLimit > this.tickInstructionLimit) {
                throw BigtonException(
                    BigtonErrorType.EXCEEDED_INSTR_LIMIT, this.currentLine
                )
            }
            when (instr.type) {
                is BigtonInstrType.SOURCE_LINE -> {
                    this.currentLine = instr.castArg<Int>(this.currentLine)
                }

                is BigtonInstrType.LOAD_VALUE -> {
                    this.pushOperand(instr.arg)
                }
                is BigtonInstrType.LOAD_TUPLE -> {
                    val n = instr.castArg<Int>(this.currentLine)
                    val tuple: List<Any?> = 0..n
                        .map { _ -> this.popOperand() }
                        .toList()
                        .reversed()
                    this.pushOperand(tuple)
                }
                is BigtonInstrType.LOAD_TUPLE_MEMBER -> {
                    val i = instr.castArg<Int>(this.currentLine)
                    val tuple = this.castPopOperand<List<Any?>>()
                    if (i >= tuple.size) {
                        throw BigtonException(
                            BigtonErrorType.TUPLE_INDEX_OOB, this.currentLine
                        )
                    }
                    this.pushOperand(tuple[i])
                }
                is BigtonInstrType.LOAD_OBJECT -> {
                    val members = instr.castArg<List<String>>(this.currentLine)
                    val obj: MutableMap<String, Any?> = members
                        .asReversed()
                        .map { m -> this.popOperand() }
                        .toMutableMap()
                    this.pushOperand(obj)
                }
                is BigtonInstrType.LOAD_OBJECT_MEMBER -> {
                    val member = instr.castArg<String>(this.currentLine)
                    val obj = this.castPopOperand<MutableMap<String, Any?>>()
                    val value: Any? = obj[member]
                    this.pushOperand(value)
                }
                is BigtonInstrType.LOAD_VARIABLE -> {
                    val name: String = instr.castArg<String>(this.currentLine)
                    val vars: Map<String, Any?> = this.forceVarScopeWith(name)
                    this.pushOperand(vars[name])
                }
                is BigtonInstrType.LOAD_MEMORY -> {
                    val address: Int = this.addressPopOperand()
                    this.pushOperand(this.memory[address])
                }

                is BigtonInstrType.ADD -> {
                    val b: Any? = this.popOperand()
                    val a: Any? = this.popOperand()
                    if (a is Long && b is Long) {
                        this.pushOperand(a + b)
                    } else if (a is Double && b is Double) {
                        this.pushOperand(a + b)
                    } else if (a is String && b is String) {
                        this.pushOperand(a + b)
                    } else {
                        throw BigtonException(
                            BigtonErrorType.INVALID_TYPE, this.currentLine
                        )
                    }
                }
                is BigtonInstrType.SUBTRACT -> this.executeBinaryNumOp(
                    i = { a, b -> a - b },
                    f = { a, b -> a - b }
                )
                is BigtonInstrType.MULTIPLY -> this.executeBinaryNumOp(
                    i = { a, b -> a * b },
                    f = { a, b -> a * b }
                )
                is BigtonInstrType.DIVIDE -> this.executeBinaryNumOp(
                    i = { a, b -> 
                        if (b == 0L) {
                            throw BigtonException(
                                BigtonErrorType.INT_DIVISION_BY_ZERO,
                                this.currentLine
                            )
                        }
                        a / b
                    },
                    f = { a, b -> a / b }
                )
                is BigtonInstrType.REMAINDER -> this.executeBinaryNumOp(
                    i = { a, b -> 
                        if (b == 0L) {
                            throw BigtonException(
                                BigtonErrorType.INT_DIVISION_BY_ZERO,
                                this.currentLine
                            )
                        }
                        a % b
                    },
                    f = { a, b -> a % b }
                )

                is BigtonInstrType.LESS_THAN -> this.executeBinaryNumOp(
                    i = { a, b -> a < b },
                    f = { a, b -> a < b }
                )
                is BigtonInstrType.LESS_THAN_EQUAL -> this.executeBinaryNumOp(
                    i = { a, b -> a <= b },
                    f = { a, b -> a <= b }
                )
                is BigtonInstrType.EQUAL -> {
                    val b: Any? = this.popOperand()
                    val a: Any? = this.popOperand()
                    this.pushOperand(
                        if (this.valuesEqual(a, b)) { 1L } else { 0L }
                    )
                }
                is BigtonInstrType.NOT_EQUAL -> {
                    val b: Any? = this.popOperand()
                    val a: Any? = this.popOperand()
                    this.pushOperand(
                        if (this.valuesEqual(a, b)) { 0L } else { 1L }
                    )
                }

                is BigtonInstrType.AND -> {
                    val b: Long = this.castPopOperand<Long>()
                    val a: Long = this.castPopOperand<Long>()
                    this.pushOperand(if (!this.isTruthy(a)) { a } else { b })
                }
                is BigtonInstrType.OR -> {
                    val b: Long = this.castPopOperand<Long>()
                    val a: Long = this.castPopOperand<Long>()
                    this.pushOperand(if (this.isTruthy(a)) { a } else { b })
                }
                is BigtonInstrType.NOT -> {
                    val x: Long = this.castPopOperand<Long>()
                    this.pushOperand(if (this.isTruthy(x)) { 0L } else { 1L })
                }

                is BigtonInstrType.STORE_EXISTING_VARIABLE -> {
                    val name: String = instr.castArg<String>(this.currentLine)
                    val vars: Map<String, Any?> = this.forceVarScopeWith(name)
                    val value: Any? = this.popOperand()
                    vars[name] = value
                }
                is BigtonInstrType.STORE_NEW_VARIABLE -> {
                    val name: String = instr.castArg<String>(this.currentLine)
                    val vars: Map<String, Any?> = this.locals.lastOrNull()
                        ?.variables ?: this.globals
                    val value: Any? = this.popOperand()
                    vars[name] = value
                }
                is BigtonInstrType.STORE_MEMORY -> {
                    val value: Any? = this.popOperand()
                    val address: Int = this.addressPopOperand()
                    this.memory[address] = value
                }
                is BigtonInstrType.STORE_OBJECT_MEMBER -> {
                    val member = instr.castArg<String>(this.currentLine)
                    val value = this.popOperand()
                    val obj = this.castPopOperand<MutableMap<String, Any?>>()
                    obj[member] = value
                }

                is BigtonInstrType.IF -> {
                    val (if_body, else_body) = instr
                        .castArg<Pair<List<BigtonInstr>, List<BigtonInstr>?>>(
                            this.currentLine
                        )
                    val cond = this.castPopOperand<Long>()
                    if (this.isTruthy(cond)) {
                        this.scopes.add(Scope(ScopeType.IF, if_body))
                    } else if (else_body != null) {
                        this.scopes.add(Scope(ScopeType.IF, else_body))
                    }
                }
                is BigtonInstrType.LOOP -> {
                    val body = instr.castArg<List<BigtonInstr>>(
                        this.currentLine
                    )
                    this.scopes.add(Scope(ScopeType.LOOP, body))
                }
                is BigtonInstrType.TICK -> {
                    val body = instr.castArg<List<BigtonInstr>>(
                        this.currentLine
                    )
                    this.scopes.add(Scope(ScopeType.TICK, body))
                }
                is BigtonInstrType.CONTINUE -> {
                    scope.current = 0
                }
                is BigtonInstrType.BREAK -> {
                    removeScope@ while (true) {
                        val removed: Scope = this.scopes.last()
                        when (removed.type) {
                            is ScopeType.IF -> {
                                this.scopes.removeLast()
                            }
                            is ScopeType.LOOP,
                            is ScopeType.TICK -> {
                                this.scopes.removeLast()
                                break@removeScope
                            }
                            is ScopeType.GLOBAL,
                            is ScopeType.FUNCTION -> {
                                break@removeScope
                            }
                        }
                    }
                }
                is BigtonInstrType.CALL -> {
                    val name = instr.castArg<String>(this.currentLine)
                    val called: Function? = this.functions.getOrNull(name)
                    if (called == null) {
                        throw BigtonException(
                            BigtonErrorType.MISSING_FUNCTION, this.currentLine
                        )
                    }
                    instructionLimit += called.instrCost
                    called.impl(this)
                    continue@fetchExec
                }
                is BigtonInstrType.RETURN -> {
                    removeScope@ while (true) {
                        val removed: Scope = this.scopes.last()
                        when (removed.type) {
                            is ScopeType.IF,
                            is ScopeType.LOOP,
                            is ScopeType.TICK -> {
                                this.scopes.removeLast()
                            }
                            is ScopeType.FUNCTION -> {
                                this.scopes.removeLast()
                                break@removeScope
                            }
                            is ScopeType.GLOBAL -> {
                                break@removeScope
                            }
                        }
                    }
                    if (this.calls.size >= 1) {
                        this.calls.removeLast()
                    }
                }
            }
            scope.current += 1
            instructionLimit += 1
        }
    }

}