
package schwalbe.ventura.bigton.runtime

import schwalbe.ventura.bigton.*

class BigtonRuntime(
    program: BigtonProgram,
    modules: List<BigtonRuntime.Module>,
    memorySize: Int,
    val tickInstructionLimit: Long,
    val maxCallDepth: Int,
    val maxTupleSize: Int
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
        var current: Int = 0,
        var numLocals: Int = 0
    )

    data class Call(
        val name: String,
        val fromLine: Int,
        val fromFile: String
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
                r.calls.add(Call(name, this.currentLine, this.currentFile))
                r.scopes.add(Scope(ScopeType.FUNCTION, body))
                if (this.calls.size > this.maxCallDepth) {
                    throw BigtonException(
                        BigtonErrorType.MAXIMUM_CALL_DEPTH,
                        this.getCurrentSource()
                    )
                }
            }
        }
        this.functions = functions
    }

    private val logs: MutableList<String> = mutableListOf()
    private val memory: Array<BigtonValue>
        = Array(memorySize) { BigtonNull }
    private val operands: MutableList<BigtonValue> = mutableListOf()
    private val scopes: MutableList<Scope> = mutableListOf(
        Scope(ScopeType.GLOBAL, program.global)
    )
    private val calls: MutableList<Call> = mutableListOf()
    private val globals: MutableMap<String, BigtonValue> = mutableMapOf()
    private val locals: MutableList<BigtonValue> = mutableListOf()
    private var currentFuncName: String = "<global>"
    private var currentLine: Int = 0
    private var currentFile: String = "<internal>"

    fun logLine(line: String) {
        this.logs.add(line)
    }

    fun pushOperand(value: BigtonValue) {
        this.operands.add(value)
    }

    fun popOperand(): BigtonValue {
        return this.operands.removeLast()
    }

    inline fun<reified T> castPopOperand(err: BigtonErrorType): T {
        val v: BigtonValue = this.popOperand()
        if (v is T) { return v }
        throw BigtonException(err, this.getCurrentSource())
    }

    fun addressPopOperand(): Int {
        val address: Long = this.castPopOperand<BigtonInt>(
            BigtonErrorType.OPERAND_NOT_INTEGER
        ).v
        if (address < 0) {
            throw BigtonException(
                BigtonErrorType.MEMORY_ADDRESS_NEGATIVE, this.getCurrentSource()
            )
        } else if (address >= this.memory.size) {
            throw BigtonException(
                BigtonErrorType.MEMORY_ADDRESS_TOO_LARGE,
                this.getCurrentSource()
            )
        }
        return address.toInt()
    }

    fun getCurrentLine(): Int = this.currentLine
    fun getCurrentFile(): String = this.currentFile
    fun getCurrentSource(): BigtonSource
        = BigtonSource(this.currentLine, this.currentFile)

    fun getStackTrace(): List<Call> = this.calls.toList()
    fun logStackTrace(error: BigtonException? = null) {
        if (!this.calls.isEmpty()) {
            this.logs.add("Stack trace (most recent call last):")
        }
        for (call in this.calls) {
            this.logs.add(" - '${call.name}' called from line ${call.fromLine} in '${call.fromFile}'")
        }
        if (error != null) {
            val src: BigtonSource = error.source
            val err: BigtonErrorType = error.error
            this.logs.add("Error on line ${src.line} in file '${src.file}':")
            this.logs.add("${err.message} (${err.id})")
        }
    }
    fun getLogString(): String = this.logs.joinToString("\n")

    private fun executeBinaryNumOp(
        i: (Long, Long) -> BigtonValue, f: (Double, Double) -> BigtonValue
    ) {
        val b: BigtonValue = this.popOperand()
        val a: BigtonValue = this.popOperand()
        this.pushOperand(when {
            a is BigtonInt && b is BigtonInt -> i(a.v, b.v)
            a is BigtonFloat && b is BigtonFloat -> f(a.v, b.v)
            else -> throw BigtonException(
                BigtonErrorType.OPERANDS_NOT_NUMBERS, this.getCurrentSource()
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

    private fun resetScope() {
        val scope: Scope = this.scopes.last()
        this.locals.subList(
            fromIndex = this.locals.size - scope.numLocals,
            toIndex = this.locals.size
        ).clear()
        scope.numLocals = 0
        scope.current = 0
    }

    private fun popScope() {
        this.resetScope()
        this.scopes.removeLast()
    }
    
    private fun popCall() {
        val call: Call = this.calls.removeLast()
        this.currentLine = call.fromLine
        this.currentFile = call.fromFile
    }

    private inline fun<reified T> castInstrArg(instr: BigtonInstr): T
        = instr.arg as? T
        ?: throw BigtonException(
            BigtonErrorType.INVALID_INSTR_ARG,
            this.getCurrentSource()
        )

    fun executeTick() {
        var instructionLimit: Long = 0
        fetchExec@ while (true) {
            if (instructionLimit > this.tickInstructionLimit) {
                throw BigtonException(
                    BigtonErrorType.EXCEEDED_INSTR_LIMIT,
                    this.getCurrentSource()
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
                        this.popScope()
                        this.popCall()
                        this.pushOperand(BigtonNull)
                    }
                    ScopeType.LOOP -> {
                        this.resetScope()
                        instructionLimit += 1
                    }
                    ScopeType.IF -> {
                        this.popScope()
                    }
                    ScopeType.TICK -> {
                        this.resetScope()
                        break@fetchExec
                    }
                }
                continue@fetchExec
            }
            when (instr.type) {
                BigtonInstrType.SOURCE_LINE -> {
                    this.currentLine = this.castInstrArg<Int>(instr)
                }
                BigtonInstrType.SOURCE_FILE -> {
                    this.currentFile = this.castInstrArg<String>(instr)
                }
                BigtonInstrType.DISCARD -> {
                    this.popOperand()
                }

                BigtonInstrType.LOAD_VALUE -> {
                    val value = this.castInstrArg<BigtonValue>(instr)
                    this.pushOperand(value)
                }
                BigtonInstrType.LOAD_TUPLE -> {
                    val n = this.castInstrArg<Int>(instr)
                    val values: List<BigtonValue> = (0..<n)
                        .map { _ -> this.popOperand() }
                        .toList()
                        .reversed()
                    val flatLen: Int = values.sumOf { when (it) {
                        is BigtonTuple -> it.flatLen
                        else -> 1
                    } }
                    if (flatLen > this.maxTupleSize) {
                        throw BigtonException(
                            BigtonErrorType.TUPLE_TOO_BIG,
                            this.getCurrentSource()
                        )
                    }
                    this.pushOperand(BigtonTuple(values, flatLen))
                }
                BigtonInstrType.LOAD_TUPLE_MEMBER -> {
                    val i = this.castInstrArg<Int>(instr)
                    val tuple = this.castPopOperand<BigtonTuple>(
                        BigtonErrorType.OPERAND_NOT_TUPLE
                    )
                    if (i >= tuple.elements.size) {
                        throw BigtonException(
                            BigtonErrorType.TUPLE_INDEX_OOB,
                            this.getCurrentSource()
                        )
                    }
                    this.pushOperand(tuple.elements[i])
                }
                BigtonInstrType.LOAD_OBJECT -> {
                    val members = this.castInstrArg<List<String>>(instr)
                    val obj: MutableMap<String, BigtonValue> = members
                        .asReversed()
                        .map { m -> Pair(m, this.popOperand()) }
                        .toMap().toMutableMap()
                    this.pushOperand(BigtonObject(obj))
                }
                BigtonInstrType.LOAD_OBJECT_MEMBER -> {
                    val member = this.castInstrArg<String>(instr)
                    val obj = this.castPopOperand<BigtonObject>(
                        BigtonErrorType.OPERAND_NOT_OBJECT
                    )
                    val value: BigtonValue = obj.members[member]
                        ?: throw BigtonException(
                            BigtonErrorType.INVALID_OBJECT_MEMBER,
                            this.getCurrentSource()
                        )
                    this.pushOperand(value)
                }
                BigtonInstrType.LOAD_GLOBAL -> {
                    val name: String = this.castInstrArg<String>(instr)
                    val value: BigtonValue = this.globals[name]
                        ?: throw BigtonException(
                            BigtonErrorType.MISSING_GLOBAL,
                            this.getCurrentSource()
                        )
                    this.pushOperand(value)
                }
                BigtonInstrType.LOAD_LOCAL -> {
                    val relIdx: Int = this.castInstrArg<Int>(instr)
                    val idx: Int = this.locals.size - 1 - relIdx
                    val value: BigtonValue = this.locals[idx]
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
                                this.getCurrentSource()
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
                                this.getCurrentSource()
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
                            this.getCurrentSource()
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

                BigtonInstrType.STORE_GLOBAL -> {
                    val name: String = this.castInstrArg<String>(instr)
                    this.globals[name] = this.popOperand()
                }
                BigtonInstrType.PUSH_LOCAL -> {
                    this.locals.add(this.popOperand())
                    scope.numLocals += 1
                }
                BigtonInstrType.STORE_LOCAL -> {
                    val relIdx: Int = this.castInstrArg<Int>(instr)
                    val idx: Int = this.locals.size - 1 - relIdx
                    this.locals[idx] = this.popOperand()
                }
                BigtonInstrType.STORE_MEMORY -> {
                    val value: BigtonValue = this.popOperand()
                    val address: Int = this.addressPopOperand()
                    this.memory[address] = value
                }
                BigtonInstrType.STORE_OBJECT_MEMBER -> {
                    val member = this.castInstrArg<String>(instr)
                    val value: BigtonValue = this.popOperand()
                    val obj: BigtonObject = this.castPopOperand<BigtonObject>(
                        BigtonErrorType.OPERAND_NOT_OBJECT
                    )
                    if (!obj.members.containsKey(member)) {
                        throw BigtonException(
                            BigtonErrorType.INVALID_OBJECT_MEMBER,
                            this.getCurrentSource()
                        )
                    }
                    obj.members[member] = value
                }

                BigtonInstrType.IF -> {
                    val (if_body, else_body) = this.castInstrArg<
                        Pair<List<BigtonInstr>, List<BigtonInstr>?>
                    >(instr)
                    val cond: BigtonValue = this.popOperand()
                    if (this.isTruthy(cond)) {
                        this.scopes.add(Scope(ScopeType.IF, if_body))
                    } else if (else_body != null) {
                        this.scopes.add(Scope(ScopeType.IF, else_body))
                    }
                }
                BigtonInstrType.LOOP -> {
                    val body = this.castInstrArg<List<BigtonInstr>>(instr)
                    this.scopes.add(Scope(ScopeType.LOOP, body))
                }
                BigtonInstrType.TICK -> {
                    val body = this.castInstrArg<List<BigtonInstr>>(instr)
                    this.scopes.add(Scope(ScopeType.TICK, body))
                }
                BigtonInstrType.CONTINUE -> {
                    this.resetScope()
                }
                BigtonInstrType.BREAK -> {
                    removeScope@ while (true) {
                        val removed: Scope = this.scopes.last()
                        when (removed.type) {
                            ScopeType.IF -> {
                                this.popScope()
                            }
                            ScopeType.LOOP,
                            ScopeType.TICK -> {
                                this.popScope()
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
                    val name = this.castInstrArg<String>(instr)
                    val called: Function = this.functions[name]
                        ?: throw BigtonException(
                            BigtonErrorType.MISSING_FUNCTION,
                            this.getCurrentSource()
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
                                this.popScope()
                            }
                            ScopeType.FUNCTION -> {
                                this.popScope()
                                break@removeScope
                            }
                            ScopeType.GLOBAL -> {
                                break@removeScope
                            }
                        }
                    }
                    if (this.calls.size >= 1) {
                        this.popCall()
                    }
                }
            }
            scope.current += 1
            instructionLimit += 1
        }
    }

}