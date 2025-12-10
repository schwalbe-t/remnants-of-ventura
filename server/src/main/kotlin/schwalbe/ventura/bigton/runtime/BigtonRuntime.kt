
package schwalbe.ventura.bigton.runtime

import schwalbe.ventura.bigton.*

class BigtonRuntime(
    program: BigtonProgram,
    modules: List<BigtonRuntime.Module>,
    memorySize: Int,
    val tickInstructionLimit: Long
) {

    data class BuiltinFunction(
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
        TICK
    }

    private data class Scope(
        val type: ScopeType,
        val body: List<BigtonRuntime>,
        var current: Int
    )


    val log: MutableList<String> = mutableListOf()

    private val memory: Array<Any?> = arrayOfNulls<Any?>(memorySize)
    private val operands: MutableList<Any?> = mutableListOf()
    private val scopes: MutableList<Scope>
        = mutableListOf(Scope(ScopeType.GLOBAL, program.global, 0))
    private var variables: MutableList<MutableMap<String, Any?>>
        = mutableListOf(mutableMapOf())
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

    fun getCurrentLine(): Int = this.currentLine

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
                        this.variables.removeLast()
                    }
                    is ScopeType.LOOP -> {
                        scope.current = 0
                    }
                    is ScopeType.TICK -> {
                        scope.current = 0
                        break@fetchExec
                    }
                }
                continue
            }
            if (instructionLimit > this.tickInstructionLimit) {
                throw BigtonException(
                    BigtonErrorType.EXCEEDED_INSTR_LIMIT,
                    this.currentLine
                )
            }
            var currentCost: Long = 1
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
                    // TODO!
                }
                is BigtonInstrType.LOAD_OBJECT_MEMBER -> {
                    // TODO!
                }
                is BigtonInstrType.LOAD_VARIABLE -> {
                    // TODO!
                }
                is BigtonInstrType.LOAD_MEMORY -> {
                    // TODO!
                }

                is BigtonInstrType.ADD -> {
                    // TODO!
                }
                is BigtonInstrType.SUBTRACT -> {
                    // TODO!
                }
                is BigtonInstrType.MULTIPLY -> {
                    // TODO!
                }
                is BigtonInstrType.DIVIDE -> {
                    // TODO!
                }
                is BigtonInstrType.REMAINDER -> {
                    // TODO!
                }

                is BigtonInstrType.LESS_THAN -> {
                    // TODO!
                }
                is BigtonInstrType.LESS_THAN_EQUAL -> {
                    // TODO!
                }
                is BigtonInstrType.EQUAL -> {
                    // TODO!
                }
                is BigtonInstrType.NOT_EQUAL -> {
                    // TODO!
                }

                is BigtonInstrType.AND -> {
                    // TODO!
                }
                is BigtonInstrType.OR -> {
                    // TODO!
                }
                is BigtonInstrType.NOT -> {
                    // TODO!
                }

                is BigtonInstrType.STORE_EXISTING_VARIABLE -> {
                    // TODO!
                }
                is BigtonInstrType.STORE_NEW_VARIABLE -> {
                    // TODO!
                }
                is BigtonInstrType.STORE_MEMORY -> {
                    // TODO!
                }
                is BigtonInstrType.STORE_OBJECT_MEMBER -> {
                    // TODO!
                }

                is BigtonInstrType.IF -> {
                    // TODO!
                }
                is BigtonInstrType.LOOP -> {
                    // TODO!
                }
                is BigtonInstrType.TICK -> {
                    // TODO!
                }
                is BigtonInstrType.CONTINUE -> {
                    // TODO!
                }
                is BigtonInstrType.BREAK -> {
                    // TODO!
                }
                is BigtonInstrType.CALL -> {
                    // TODO!
                }
                is BigtonInstrType.RETURN -> {
                    // TODO!
                }
            }
            scope.current += 1
            instructionLimit += currentCost
        }
    }

}