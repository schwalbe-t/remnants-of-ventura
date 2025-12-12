
package schwalbe.ventura.bigton.runtime

import schwalbe.ventura.bigton.*

class BigtonRuntime(
    program: BigtonProgram,
    modules: List<BigtonRuntime.Module>,
    memorySize: Int,
    val tickInstructionLimit: Long
) {

    data class Function(
        val cost: Long,
        val argc: Int,
        val impl: (BigtonRuntime) -> Unit,
    )

    data class Module(
        val builtinFunctions: Map<String, Function>
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
        val body: List<BigtonInstr>,
        var current: Int = 0
    )

    private data class Call(
        val name: String,
        val fromLine: Int,
        val variables: MutableMap<String, BigtonValue> = mutableMapOf()
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
            functions[name] = Function(cost = 0, argc = -1) { r ->
                r.calls.add(Call(name, this.currentLine))
                r.scopes.add(Scope(ScopeType.FUNCTION, body))
            }
        }
        this.functions = functions
    }

    val logs: MutableList<String> = mutableListOf()

    private val memory: Array<BigtonValue>
        = Array(memorySize) { BigtonNull }
    private val operands: MutableList<BigtonValue> = mutableListOf()
    private val scopes: MutableList<Scope> = mutableListOf(
        Scope(ScopeType.GLOBAL, program.global)
    )
    private val calls: MutableList<Call> = mutableListOf()
    private val globals: MutableMap<String, BigtonValue> = mutableMapOf()
    private var currentFuncName: String = "<global>"
    private var currentLine: Int = 0

    fun pushOperand(value: BigtonValue) {
        this.operands.add(value)
    }

    fun popOperand(): BigtonValue {
        if (this.operands.size >= 1) {
            return this.operands.removeLast()
        }
        throw BigtonException(
            BigtonErrorType.MISSING_OPERAND, this.currentLine
        )
    }

    inline fun<reified T> castPopOperand(err: BigtonErrorType): T {
        val v: BigtonValue = this.popOperand()
        if (v is T) { return v }
        throw BigtonException(err, this.getCurrentLine())
    }

    fun addressPopOperand(): Int {
        val address: Long = this.castPopOperand<BigtonInt>(
            BigtonErrorType.OPERAND_NOT_INTEGER
        ).v
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

    private fun findVarScopeWith(name: String)
    : MutableMap<String, BigtonValue> {
        val locals: MutableMap<String, BigtonValue>?
            = this.calls.lastOrNull()?.variables
        if (locals != null && locals.containsKey(name)) { return locals }
        if (this.globals.containsKey(name)) { return this.globals }
        throw BigtonException(
            BigtonErrorType.MISSING_VARIABLE, this.currentLine
        )
    }

    private fun executeBinaryNumOp(
        i: (Long, Long) -> BigtonValue, f: (Double, Double) -> BigtonValue
    ) {
        val b: BigtonValue = this.popOperand()
        val a: BigtonValue = this.popOperand()
        this.pushOperand(when {
            a is BigtonInt && b is BigtonInt -> i(a.v, b.v)
            a is BigtonFloat && b is BigtonFloat -> f(a.v, b.v)
            else -> throw BigtonException(
                BigtonErrorType.OPERANDS_NOT_NUMBERS, this.currentLine
            ) 
        })
    }

    private fun valuesEqual(a: BigtonValue, b: BigtonValue): Boolean = when {
        a is BigtonNull   && b is BigtonNull   -> true
        a is BigtonInt    && b is BigtonInt    -> a.v == b.v
        a is BigtonFloat  && b is BigtonFloat  -> a.v == b.v
        a is BigtonString && b is BigtonString -> a.v == b.v
        a is BigtonTuple  && b is BigtonTuple
            -> a.elements.size == b.elements.size
            && a.elements.indices
                .all { i -> this.valuesEqual(a.elements[i], b.elements[i]) }
        a is BigtonObject && b is BigtonObject
            -> a.members.keys == b.members.keys
            && a.members.keys
                .all { k -> this.valuesEqual(a.members[k]!!, b.members[k]!!) }
        else -> false
    }

    private fun isTruthy(x: BigtonValue): Boolean = when (x) {
        is BigtonNull -> false
        is BigtonInt -> x.v != 0L
        is BigtonFloat -> x.v != 0.0 && !x.v.isNaN()
        is BigtonString,
        is BigtonTuple,
        is BigtonObject -> true
    }

    fun executeTick() {
        var instructionLimit: Long = 0
        fetchExec@ while (true) {
            if (instructionLimit > this.tickInstructionLimit) {
                throw BigtonException(
                    BigtonErrorType.EXCEEDED_INSTR_LIMIT, this.currentLine
                )
            }
            val scope: Scope = this.scopes.last()
            val instr: BigtonInstr? = scope.body.getOrNull(scope.current)
            if (instr == null) {
                when (scope.type) {
                    ScopeType.GLOBAL -> {
                        break@fetchExec
                    }
                    ScopeType.FUNCTION -> {
                        this.scopes.removeLast()
                        this.calls.removeLast()
                        this.pushOperand(BigtonNull)
                    }
                    ScopeType.LOOP -> {
                        scope.current = 0
                        instructionLimit += 1
                    }
                    ScopeType.IF -> {
                        this.scopes.removeLast()
                    }
                    ScopeType.TICK -> {
                        scope.current = 0
                        break@fetchExec
                    }
                }
                continue@fetchExec
            }
            when (instr.type) {
                BigtonInstrType.SOURCE_LINE -> {
                    this.currentLine = instr.castArg<Int>(this.currentLine)
                }

                BigtonInstrType.LOAD_VALUE -> {
                    val value = instr.castArg<BigtonValue>(this.currentLine)
                    this.pushOperand(value)
                }
                BigtonInstrType.LOAD_TUPLE -> {
                    val n = instr.castArg<Int>(this.currentLine)
                    val tuple: List<BigtonValue> = (0..<n)
                        .map { _ -> this.popOperand() }
                        .toList()
                        .reversed()
                    this.pushOperand(BigtonTuple(tuple))
                }
                BigtonInstrType.LOAD_TUPLE_MEMBER -> {
                    val i = instr.castArg<Int>(this.currentLine)
                    val tuple = this.castPopOperand<BigtonTuple>(
                        BigtonErrorType.OPERAND_NOT_TUPLE
                    )
                    if (i >= tuple.elements.size) {
                        throw BigtonException(
                            BigtonErrorType.TUPLE_INDEX_OOB, this.currentLine
                        )
                    }
                    this.pushOperand(tuple.elements[i])
                }
                BigtonInstrType.LOAD_OBJECT -> {
                    val members = instr.castArg<List<String>>(this.currentLine)
                    val obj: MutableMap<String, BigtonValue> = members
                        .asReversed()
                        .map { m -> Pair(m, this.popOperand()) }
                        .toMap().toMutableMap()
                    this.pushOperand(BigtonObject(obj))
                }
                BigtonInstrType.LOAD_OBJECT_MEMBER -> {
                    val member = instr.castArg<String>(this.currentLine)
                    val obj = this.castPopOperand<BigtonObject>(
                        BigtonErrorType.OPERAND_NOT_OBJECT
                    )
                    val value: BigtonValue = obj.members[member]
                        ?: throw BigtonException(
                            BigtonErrorType.INVALID_OBJECT_MEMBER,
                            this.currentLine
                        )
                    this.pushOperand(value)
                }
                BigtonInstrType.LOAD_VARIABLE -> {
                    val name: String = instr.castArg<String>(this.currentLine)
                    val vars: Map<String, BigtonValue>
                        = this.findVarScopeWith(name)
                    // enforced by 'findVarScopeName'
                    val value: BigtonValue = vars[name]!!
                    this.pushOperand(value)
                }
                BigtonInstrType.LOAD_MEMORY -> {
                    val address: Int = this.addressPopOperand()
                    this.pushOperand(this.memory[address])
                }

                BigtonInstrType.ADD -> this.executeBinaryNumOp(
                    i = { a, b -> BigtonInt(a + b) },
                    f = { a, b -> BigtonFloat(a + b) }
                )
                BigtonInstrType.SUBTRACT -> this.executeBinaryNumOp(
                    i = { a, b -> BigtonInt(a - b) },
                    f = { a, b -> BigtonFloat(a - b) }
                )
                BigtonInstrType.MULTIPLY -> this.executeBinaryNumOp(
                    i = { a, b -> BigtonInt(a * b) },
                    f = { a, b -> BigtonFloat(a * b) }
                )
                BigtonInstrType.DIVIDE -> this.executeBinaryNumOp(
                    i = { a, b -> 
                        if (b == 0L) {
                            throw BigtonException(
                                BigtonErrorType.INT_DIVISION_BY_ZERO,
                                this.currentLine
                            )
                        }
                        BigtonInt(a / b)
                    },
                    f = { a, b -> BigtonFloat(a / b) }
                )
                BigtonInstrType.REMAINDER -> this.executeBinaryNumOp(
                    i = { a, b -> 
                        if (b == 0L) {
                            throw BigtonException(
                                BigtonErrorType.INT_DIVISION_BY_ZERO,
                                this.currentLine
                            )
                        }
                        BigtonInt(a % b)
                    },
                    f = { a, b -> BigtonFloat(a % b) }
                )
                BigtonInstrType.NEGATE -> {
                    val x: BigtonValue = this.popOperand()
                    this.pushOperand(when (x) {
                        is BigtonInt -> BigtonInt(-x.v)
                        is BigtonFloat -> BigtonFloat(-x.v)
                        else -> throw BigtonException(
                            BigtonErrorType.OPERANDS_NOT_NUMBERS,
                            this.currentLine
                        ) 
                    })
                }

                BigtonInstrType.LESS_THAN -> this.executeBinaryNumOp(
                    i = { a, b -> BigtonInt(if (a < b) { 1L } else { 0L }) },
                    f = { a, b -> BigtonInt(if (a < b) { 1L } else { 0L }) }
                )
                BigtonInstrType.LESS_THAN_EQUAL -> this.executeBinaryNumOp(
                    i = { a, b -> BigtonInt(if (a <= b) { 1L } else { 0L }) },
                    f = { a, b -> BigtonInt(if (a <= b) { 1L } else { 0L }) }
                )
                BigtonInstrType.GREATER_THAN -> this.executeBinaryNumOp(
                    i = { a, b -> BigtonInt(if (a > b) { 1L } else { 0L }) },
                    f = { a, b -> BigtonInt(if (a > b) { 1L } else { 0L }) }
                )
                BigtonInstrType.GREATER_THAN_EQUAL -> this.executeBinaryNumOp(
                    i = { a, b -> BigtonInt(if (a >= b) { 1L } else { 0L }) },
                    f = { a, b -> BigtonInt(if (a >= b) { 1L } else { 0L }) }
                )
                BigtonInstrType.EQUAL -> {
                    val b: BigtonValue = this.popOperand()
                    val a: BigtonValue = this.popOperand()
                    val r = if (this.valuesEqual(a, b)) { 1L } else { 0L }
                    this.pushOperand(BigtonInt(r))
                }
                BigtonInstrType.NOT_EQUAL -> {
                    val b: BigtonValue = this.popOperand()
                    val a: BigtonValue = this.popOperand()
                    val r = if (this.valuesEqual(a, b)) { 0L } else { 1L }
                    this.pushOperand(BigtonInt(r))
                }

                BigtonInstrType.AND -> {
                    val b: BigtonValue = this.popOperand()
                    val a: BigtonValue = this.popOperand()
                    this.pushOperand(if (!this.isTruthy(a)) { a } else { b })
                }
                BigtonInstrType.OR -> {
                    val b: BigtonValue = this.popOperand()
                    val a: BigtonValue = this.popOperand()
                    this.pushOperand(if (this.isTruthy(a)) { a } else { b })
                }
                BigtonInstrType.NOT -> {
                    val x: BigtonValue = this.popOperand()
                    val r: Long = if (this.isTruthy(x)) { 0L } else { 1L }
                    this.pushOperand(BigtonInt(r))
                }

                BigtonInstrType.STORE_EXISTING_VARIABLE -> {
                    val name: String = instr.castArg<String>(this.currentLine)
                    val vars: MutableMap<String, BigtonValue>
                        = this.findVarScopeWith(name)
                    vars[name] = this.popOperand()
                }
                BigtonInstrType.STORE_NEW_VARIABLE -> {
                    val name: String = instr.castArg<String>(this.currentLine)
                    val vars: MutableMap<String, BigtonValue>
                        = this.calls.lastOrNull()?.variables ?: this.globals
                    vars[name] = this.popOperand()
                }
                BigtonInstrType.STORE_MEMORY -> {
                    val value: BigtonValue = this.popOperand()
                    val address: Int = this.addressPopOperand()
                    this.memory[address] = value
                }
                BigtonInstrType.STORE_OBJECT_MEMBER -> {
                    val member = instr.castArg<String>(this.currentLine)
                    val value: BigtonValue = this.popOperand()
                    val obj: BigtonObject = this.castPopOperand<BigtonObject>(
                        BigtonErrorType.OPERAND_NOT_OBJECT
                    )
                    if (!obj.members.containsKey(member)) {
                        throw BigtonException(
                            BigtonErrorType.INVALID_OBJECT_MEMBER,
                            this.currentLine
                        )
                    }
                    obj.members[member] = value
                }

                BigtonInstrType.IF -> {
                    val (if_body, else_body) = instr
                        .castArg<Pair<List<BigtonInstr>, List<BigtonInstr>?>>(
                            this.currentLine
                        )
                    val cond: BigtonValue = this.popOperand()
                    if (this.isTruthy(cond)) {
                        this.scopes.add(Scope(ScopeType.IF, if_body))
                    } else if (else_body != null) {
                        this.scopes.add(Scope(ScopeType.IF, else_body))
                    }
                }
                BigtonInstrType.LOOP -> {
                    val body = instr.castArg<List<BigtonInstr>>(
                        this.currentLine
                    )
                    this.scopes.add(Scope(ScopeType.LOOP, body))
                }
                BigtonInstrType.TICK -> {
                    val body = instr.castArg<List<BigtonInstr>>(
                        this.currentLine
                    )
                    this.scopes.add(Scope(ScopeType.TICK, body))
                }
                BigtonInstrType.CONTINUE -> {
                    scope.current = 0
                }
                BigtonInstrType.BREAK -> {
                    removeScope@ while (true) {
                        val removed: Scope = this.scopes.last()
                        when (removed.type) {
                            ScopeType.IF -> {
                                this.scopes.removeLast()
                            }
                            ScopeType.LOOP,
                            ScopeType.TICK -> {
                                this.scopes.removeLast()
                                break@removeScope
                            }
                            ScopeType.GLOBAL,
                            ScopeType.FUNCTION -> {
                                break@removeScope
                            }
                        }
                    }
                }
                BigtonInstrType.CALL -> {
                    val name = instr.castArg<String>(this.currentLine)
                    val called: Function = this.functions[name]
                        ?: throw BigtonException(
                            BigtonErrorType.MISSING_FUNCTION, this.currentLine
                        )
                    scope.current += 1
                    instructionLimit += called.cost
                    called.impl(this)
                    continue@fetchExec
                }
                BigtonInstrType.RETURN -> {
                    removeScope@ while (true) {
                        val removed: Scope = this.scopes.last()
                        when (removed.type) {
                            ScopeType.IF,
                            ScopeType.LOOP,
                            ScopeType.TICK -> {
                                this.scopes.removeLast()
                            }
                            ScopeType.FUNCTION -> {
                                this.scopes.removeLast()
                                break@removeScope
                            }
                            ScopeType.GLOBAL -> {
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