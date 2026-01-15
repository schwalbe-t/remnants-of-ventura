
package schwalbe.ventura.bigton

private class IdBank<T> {
    
    private var nextId: Int = 0
    private val idToValue: MutableList<T> = mutableListOf()
    private val valueToId: MutableMap<T, Int> = mutableMapOf()
    
    val values: List<T> = this.idToValue
    
    operator fun get(value: T): Int {
        val existingId: Int? = this.valueToId[value]
        if (existingId != null) { return existingId }
        val newId: Int = this.nextId
        this.idToValue.add(value)
        this.valueToId[value] = newId
        this.nextId += 1
        return newId
    }
    
}

/**
 * Mirror of 'bigton_instr_type_t' in 'ir.h'
 */
private enum class InstrType {
    SOURCE_LINE,
    SOURCE_FILE,
    DISCARD,
    
    LOAD_NULL,
    LOAD_INT,
    LOAD_FLOAT,
    LOAD_STRING,
    LOAD_TUPLE,
    LOAD_OBJECT,
    LOAD_ARRAY,
    
    LOAD_TUPLE_MEMBER,
    LOAD_OBJECT_MEMBER,
    LOAD_ARRAY_ELEMENT,
    LOAD_GLOBAL,
    LOAD_LOCAL,
    
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    REMAINDER,
    NEGATE,
    
    LESS_THAN,
    LESS_THAN_EQUAL,
    GREATER_THAN,
    GREATER_THAN_EQUAL,
    EQUAL,
    NOT_EQUAL,
    
    AND,
    OR,
    NOT,
    
    STORE_GLOBAL,
    PUSH_LOCAL,
    STORE_LOCAL,
    STORE_OBJECT_MEMBER,
    STORE_ARRAY_ELEMENT,
    
    IF,
    LOOP,
    TICK,
    CONTINUE,
    BREAK,
    CALL,
    CALL_BUILTIN,
    RETURN
}

private data class ProgramBuilder(
    val strings: IdBank<String> = IdBank(),
    val shapes: IdBank<List<String>> = IdBank(),
    val instrTypes: MutableList<InstrType> = mutableListOf(),
    val instrArgs: BinaryWriter = BinaryWriter()
)

private fun ProgramBuilder.child() = ProgramBuilder(
    strings = this.strings,
    shapes = this.shapes
)

private fun ProgramBuilder.append(child: ProgramBuilder) {
    this.instrTypes.addAll(child.instrTypes)
    this.instrArgs.putBytes(child.instrArgs.output.toByteArray())
}

private fun ProgramBuilder.addNoArgInstr(t: InstrType) {
    this.instrTypes.add(t)
    this.instrArgs.putLong(0) // padding
}

private fun collectFunctions(ast: List<BigtonAst>): Map<String, BigtonAst>
    = ast
    .filter { n -> n.type == BigtonAstType.FUNCTION }
    .map { f -> f.castArg<BigtonAstFunction>().name to f }
    .toMap()

private fun collectGlobalVars(ast: List<BigtonAst>): Set<String>
    = ast
    .filter { n -> n.type == BigtonAstType.VARIABLE }
    .map { v -> v.castArg<String>() }
    .toSet()

private fun collectGlobalStatements(ast: List<BigtonAst>): List<BigtonAst>
    = ast
    .filter { n -> n.type != BigtonAstType.FUNCTION }
    .toList()

private data class ProgramSymbols(
    val builtinFunctions: BigtonBuiltinFunctions,
    val functions: Map<String, BigtonAst>,
    val functionIds: Map<String, Int>,
    val globalVars: Set<String>,
    val globalIds: Map<String, Int>
)

private class SourceTracker(
    var line: Int = -1,
    var file: String = "<unknown>"
)

private fun SourceTracker.reset() {
    this.line = -1
    this.file = "<unknown>"
}

private fun SourceTracker.setLine(
    newLine: Int, program: ProgramBuilder
) {
    if (newLine == this.line) { return }
    this.line = newLine
    program.instrTypes.add(InstrType.SOURCE_LINE)
    program.instrArgs.putInt(newLine + 1)
    program.instrArgs.alignTo(8)
}

private fun SourceTracker.setFile(
    newFile: String, program: ProgramBuilder
) {
    if (newFile == this.file) { return }
    this.file = newFile
    program.instrTypes.add(InstrType.SOURCE_FILE)
    program.instrArgs.putInt(program.strings[newFile])
    program.instrArgs.alignTo(8)
}

private fun SourceTracker.toSource(): BigtonSource
    = BigtonSource(this.line, this.file)

private data class ProgramContext(
    val currentSource: SourceTracker,
    val symbols: ProgramSymbols,
    val features: Set<BigtonFeature>,
    val modules: List<BigtonModule>
)

private fun ProgramContext.assertFeatureSupported(feature: BigtonFeature) {
    if (feature in this.features) { return }
    throw BigtonException(
        BigtonErrorType.FEATURE_UNSUPPORTED, this.currentSource.toSource()
    )
}

private fun ProgramContext.findFunctionArgc(name: String): Pair<Int, Boolean>? {
    val userFun: BigtonAst? = this.symbols.functions[name]
    if (userFun != null) {
        val argc: Int = userFun.castArg<BigtonAstFunction>().argNames.size
        return Pair(argc, false)
    }
    val builtinModule: BigtonModule? = this.modules
        .find { m -> name in m.functions.keys }
    if (builtinModule != null) {
        val argc: Int = builtinModule.functions[name]!!.argc
        return Pair(argc, true)
    }
    return null
}

// var a [0] (numLocals = 1)
// a -> -numLocals + 0 -> -1

// var b [1] (numLocals = 2)
// a -> -numLocals + 0 -> -2
// b -> -numLocals + 1 -> -1

// var c [2] (numLocals = 3)
// a -> -numLocals + 0 -> -3
// b -> -numLocals + 1 -> -2
// c -> -numLocals + 2 -> -1 

private class ScopeContext(
    val inGlobal: Boolean,
    val inFunction: Boolean,
    val inLoop: Boolean,
    val locals: MutableMap<String, Int>,
    var numLocals: Int,
    val program: ProgramContext
)

private fun ScopeContext.inChildScope(
    isLoop: Boolean = this.inLoop
) = ScopeContext(
    inGlobal = false,
    inFunction = this.inFunction,
    inLoop = isLoop,
    locals = this.locals.toMutableMap(),
    numLocals = this.numLocals,
    program = this.program
)

private fun ScopeContext.getLocalRelIndex(name: String): Int? {
    val idx: Int? = this.locals[name]
    if (idx == null) { return idx }
    return this.numLocals - 1 - idx
}

private fun ScopeContext.assertInFunction() {
    if (this.inFunction) { return }
    throw BigtonException(
        BigtonErrorType.RETURN_OUTSIDE_FUNCTION,
        this.program.currentSource.toSource()
    )
}

private fun ScopeContext.assertInLoop() {
    if (this.inLoop) { return }
    throw BigtonException(
        BigtonErrorType.LOOP_CONTROLS_OUTSIDE_LOOP,
        this.program.currentSource.toSource()
    )
}

private fun generateExpression(
    ast: BigtonAst, ctx: ScopeContext, program: ProgramBuilder
) {
    ctx.program.currentSource.setLine(ast.source.line, program)
    ctx.program.currentSource.setFile(ast.source.file, program)
    for (child in ast.children) {
        generateExpression(child, ctx, program)
    }
    when (ast.type) {
        BigtonAstType.IDENTIFIER -> {
            val name = ast.castArg<String>()
            val localIdx: Int? = ctx.getLocalRelIndex(name)
            if (localIdx != null) {
                program.instrTypes.add(InstrType.LOAD_LOCAL)
                program.instrArgs.putInt(localIdx)
                program.instrArgs.alignTo(8)
            } else if (name in ctx.program.symbols.globalVars) {
                val id: Int = ctx.program.symbols.globalIds[name]!!
                program.instrTypes.add(InstrType.LOAD_GLOBAL)
                program.instrArgs.putInt(id)
                program.instrArgs.alignTo(8)
            } else {
                throw BigtonException(
                    BigtonErrorType.UNKNOWN_VARIABLE, ast.source
                )
            }
        }
        BigtonAstType.NULL_LITERAL -> {
            program.addNoArgInstr(InstrType.LOAD_NULL)
        }
        BigtonAstType.INT_LITERAL -> {
            program.instrTypes.add(InstrType.LOAD_INT)
            program.instrArgs.putLong(ast.castArg<String>().toLong())
        }
        BigtonAstType.FLOAT_LITERAL -> {
            ctx.program.assertFeatureSupported(BigtonFeature.FPU_MODULE)
            program.instrTypes.add(InstrType.LOAD_FLOAT)
            program.instrArgs.putDouble(ast.castArg<String>().toDouble())
        }
        BigtonAstType.STRING_LITERAL -> {
            val strId: Int = program.strings[ast.castArg<String>()]
            program.instrTypes.add(InstrType.LOAD_STRING)
            program.instrArgs.putInt(strId)
            program.instrArgs.alignTo(8)
        }
        BigtonAstType.TUPLE_LITERAL -> {
            program.instrTypes.add(InstrType.LOAD_TUPLE)
            program.instrArgs.putInt(ast.children.size)
            program.instrArgs.alignTo(8)
        }
        BigtonAstType.OBJECT_LITERAL -> {
            ctx.program.assertFeatureSupported(BigtonFeature.OBJECTS)
            val shape: List<String> = ast.castArg<List<String>>()
            val shapeId: Int = program.shapes[shape]
            program.instrTypes.add(InstrType.LOAD_OBJECT)
            program.instrArgs.putInt(shapeId)
            program.instrArgs.alignTo(8)
        }
        BigtonAstType.CALL -> {
            val name = ast.castArg<String>()
            val (argc, isBuiltin) = ctx.program.findFunctionArgc(name)
                ?: throw BigtonException(
                    BigtonErrorType.UNKNOWN_FUNCTION, ast.source
                )
            if (ast.children.size < argc) {
                throw BigtonException(
                    BigtonErrorType.TOO_FEW_CALL_ARGS, ast.source
                )
            }
            if (ast.children.size > argc) {
                throw BigtonException(
                    BigtonErrorType.TOO_MANY_CALL_ARGS, ast.source
                )
            }
            if (isBuiltin) {
                val id: Int = ctx.program.symbols.builtinFunctions
                    .functionIds[name]!!
                program.instrTypes.add(InstrType.CALL_BUILTIN)
                program.instrArgs.putInt(id)
                program.instrArgs.alignTo(8)
            } else {
                val id: Int = ctx.program.symbols.functionIds[name]!!
                program.instrTypes.add(InstrType.CALL)
                program.instrArgs.putInt(id)
                program.instrArgs.alignTo(8)
            }
        }
        BigtonAstType.TUPLE_MEMBER -> {
            program.instrTypes.add(InstrType.LOAD_TUPLE_MEMBER)
            program.instrArgs.putInt(ast.castArg<Int>())
            program.instrArgs.alignTo(8)
        }
        BigtonAstType.OBJECT_MEMBER -> {
            ctx.program.assertFeatureSupported(BigtonFeature.OBJECTS)
            val nameId: Int = program.strings[ast.castArg<String>()]
            program.instrTypes.add(InstrType.LOAD_OBJECT_MEMBER)
            program.instrArgs.putInt(nameId)
            program.instrArgs.alignTo(8)
        }
        BigtonAstType.DEREF -> {
            throw IllegalStateException("no runtime support")
        }
        BigtonAstType.NOT
            -> program.addNoArgInstr(InstrType.NOT)
        BigtonAstType.NEGATE
            -> program.addNoArgInstr(InstrType.NEGATE)
        BigtonAstType.ADD
            -> program.addNoArgInstr(InstrType.ADD)
        BigtonAstType.SUBTRACT
            -> program.addNoArgInstr(InstrType.SUBTRACT)
        BigtonAstType.MULTIPLY
            -> program.addNoArgInstr(InstrType.MULTIPLY)
        BigtonAstType.DIVIDE
            -> program.addNoArgInstr(InstrType.DIVIDE)
        BigtonAstType.REMAINDER
            -> program.addNoArgInstr(InstrType.REMAINDER)
        BigtonAstType.LESS_THAN
            -> program.addNoArgInstr(InstrType.LESS_THAN)
        BigtonAstType.GREATER_THAN
            -> program.addNoArgInstr(InstrType.GREATER_THAN)
        BigtonAstType.LESS_THAN_EQUAL
            -> program.addNoArgInstr(InstrType.LESS_THAN_EQUAL)
        BigtonAstType.GREATER_THAN_EQUAL
            -> program.addNoArgInstr(InstrType.GREATER_THAN_EQUAL)
        BigtonAstType.EQUAL
            -> program.addNoArgInstr(InstrType.EQUAL)
        BigtonAstType.NOT_EQUAL
            -> program.addNoArgInstr(InstrType.NOT_EQUAL)
        BigtonAstType.AND
            -> program.addNoArgInstr(InstrType.AND)
        BigtonAstType.OR
            -> program.addNoArgInstr(InstrType.OR)
        else -> throw BigtonException(
            BigtonErrorType.UNHANDLED_AST_TYPE, ast.source
        )
    })
}

private fun generateStatement(
    ast: BigtonAst, ctx: ScopeContext, instrs: MutableList<BigtonInstr>
) {
    ctx.program.currentSource.setLine(ast.source.line, instrs)
    ctx.program.currentSource.setFile(ast.source.file, instrs)
    when (ast.type) {
        BigtonAstType.ASSIGNMENT -> {
            val dest: BigtonAst = ast.children[0]
            val value: BigtonAst = ast.children[1]
            when (dest.type) {
                BigtonAstType.IDENTIFIER -> {
                    generateExpression(value, ctx, instrs)
                    val name: String = dest.castArg<String>()
                    val localIdx: Int? = ctx.getLocalRelIndex(name)
                    instrs.add(if (localIdx != null) {
                        BigtonInstr(BigtonInstrType.STORE_LOCAL, localIdx)
                    } else if (name in ctx.program.symbols.globalVars) {
                        BigtonInstr(BigtonInstrType.STORE_GLOBAL, name)
                    } else {
                        throw BigtonException(
                            BigtonErrorType.UNKNOWN_VARIABLE, ast.source
                        )
                    })
                }
                BigtonAstType.DEREF -> {
                    ctx.program.assertFeatureSupported(BigtonFeature.RAM_MODULE)
                    generateExpression(dest.children[0], ctx, instrs)
                    generateExpression(value, ctx, instrs)
                    instrs.add(BigtonInstr(BigtonInstrType.STORE_MEMORY))
                }
                BigtonAstType.OBJECT_MEMBER -> {
                    ctx.program.assertFeatureSupported(BigtonFeature.OBJECTS)
                    val member: String = dest.castArg<String>()
                    generateExpression(dest.children[0], ctx, instrs)
                    generateExpression(value, ctx, instrs)
                    instrs.add(BigtonInstr(
                        BigtonInstrType.STORE_OBJECT_MEMBER, member
                    ))
                }
                else -> throw BigtonException(
                    BigtonErrorType.ASSIGNMENT_TO_CONST, ast.source
                )
            }
        }
        BigtonAstType.IF -> {
            val cond: BigtonAst = ast.children[0]
            val (if_ast, else_ast) = ast.castArg<
                Pair<List<BigtonAst>, List<BigtonAst>?>
            >()
            generateExpression(cond, ctx, instrs)
            val if_body: List<BigtonInstr>
                = generateStatementList(if_ast, ctx.inChildScope())
            val else_body: List<BigtonInstr>? =
                if (else_ast == null) { null }
                else { generateStatementList(else_ast, ctx.inChildScope()) }
            instrs.add(BigtonInstr(
                BigtonInstrType.IF, Pair(if_body, else_body)
            ))
        }
        BigtonAstType.LOOP -> {
            val body_ast = ast.castArg<List<BigtonAst>>()
            val body: List<BigtonInstr> = generateStatementList(
                body_ast, ctx.inChildScope(isLoop = true)
            )
            instrs.add(BigtonInstr(BigtonInstrType.LOOP, body))
        }
        BigtonAstType.TICK -> {
            val body_ast = ast.castArg<List<BigtonAst>>()
            val body: List<BigtonInstr> = generateStatementList(
                body_ast, ctx.inChildScope(isLoop = true)
            )
            instrs.add(BigtonInstr(BigtonInstrType.TICK, body))
        }
        BigtonAstType.WHILE -> {
            val cond: BigtonAst = ast.children[0]
            val body_ast = ast.castArg<List<BigtonAst>>()
            // The following BIGTON source...:
            //
            //     while x { ... }
            //
            // ...is converted to the following BIGTON runtime instructions:
            // 
            //     LOOP {
            //         instructionsOf(x)
            //         NOT
            //         IF { BREAK }
            //         ...
            //     }
            //
            val body: MutableList<BigtonInstr> = mutableListOf()
            generateExpression(cond, ctx, body)
            body.add(BigtonInstr(BigtonInstrType.NOT))
            body.add(BigtonInstr(
                BigtonInstrType.IF,
                Pair<List<BigtonInstr>, List<BigtonInstr>?>(
                    listOf(BigtonInstr(BigtonInstrType.BREAK)),
                    null
                )
            ))
            body.addAll(generateStatementList(
                body_ast, ctx.inChildScope(isLoop = true)
            ))
            instrs.add(BigtonInstr(BigtonInstrType.LOOP, body))
        }
        BigtonAstType.CONTINUE -> {
            ctx.assertInLoop()
            instrs.add(BigtonInstr(BigtonInstrType.CONTINUE))
        }
        BigtonAstType.BREAK -> {
            ctx.assertInLoop()
            instrs.add(BigtonInstr(BigtonInstrType.BREAK))
        }
        BigtonAstType.RETURN -> {
            ctx.assertInFunction()
            val value: BigtonAst = ast.children[0]
            generateExpression(value, ctx, instrs)
            instrs.add(BigtonInstr(BigtonInstrType.RETURN))
        }
        BigtonAstType.VARIABLE -> {
            val name: String = ast.castArg<String>()
            val value: BigtonAst = ast.children[0]
            generateExpression(value, ctx, instrs)
            instrs.add(if (ctx.inGlobal) {
                BigtonInstr(BigtonInstrType.STORE_GLOBAL, name)
            } else {
                val idx: Int = ctx.numLocals
                ctx.locals[name] = idx
                ctx.numLocals += 1
                BigtonInstr(BigtonInstrType.PUSH_LOCAL)
            })
        }
        BigtonAstType.FUNCTION -> throw BigtonException(
            BigtonErrorType.FUNCTION_INSIDE_FUNCTION, ast.source
        )
        else -> {
            generateExpression(ast, ctx, instrs)
            instrs.add(BigtonInstr(BigtonInstrType.DISCARD))
        }
    }
}

private fun generateStatementList(
    ast: List<BigtonAst>, ctx: ScopeContext
): List<BigtonInstr> {
    val instrs = mutableListOf<BigtonInstr>()
    ast.forEach { n -> generateStatement(n, ctx, instrs) }
    return instrs
}

private fun generateFunction(
    ast: BigtonAst, ctx: ProgramContext
): List<BigtonInstr> {
    ctx.assertFeatureSupported(BigtonFeature.CUSTOM_FUNCTIONS)
    ctx.currentSource.reset()
    val function: BigtonAstFunction = ast.castArg<BigtonAstFunction>()
    val instrs = mutableListOf<BigtonInstr>()
    // When calling, we push the arguments in normal order onto the stack,
    // then execute the function body.
    // This means that in the function, we need to pop the arguments into
    // variables in the REVERSE order (since the last argument will have
    // been pushed onto the stack by the caller LAST and will therefore be
    // popped next)
    val params: MutableMap<String, Int> = mutableMapOf()
    for ((i, n) in function.argNames.asReversed().withIndex()) {
        instrs.add(BigtonInstr(BigtonInstrType.PUSH_LOCAL))
        params[n] = i
    }
    val scope = ScopeContext(
        inGlobal = false,
        inFunction = true,
        inLoop = false,
        locals = params,
        numLocals = function.argNames.size,
        program = ctx
    )
    function.body.forEach { n -> generateStatement(n, scope, instrs) }
    return instrs
}

private fun generateGlobal(
    ast: List<BigtonAst>, ctx: ProgramContext
): List<BigtonInstr> {
    ctx.currentSource.reset()
    val instrs = mutableListOf<BigtonInstr>()
    for (globalName in ctx.symbols.globalVars) {
        instrs.add(BigtonInstr(BigtonInstrType.LOAD_VALUE, BigtonNull))
        instrs.add(BigtonInstr(BigtonInstrType.STORE_GLOBAL, globalName))
    }
    val scope = ScopeContext(
        inGlobal = true,
        inFunction = false,
        inLoop = false,
        locals = mutableMapOf(),
        numLocals = 0,
        program = ctx
    )
    ast.forEach { n -> generateStatement(n, scope, instrs) }
    return instrs
}

fun generateProgram(
    ast: List<BigtonAst>,
    features: Set<BigtonFeature>,
    modules: List<BigtonRuntime.Module>
): BigtonProgram {
    val functionAsts: Map<String, BigtonAst> = collectFunctions(ast)
    val globalVars: Set<String> = collectGlobalVars(ast)
    val globalAst: List<BigtonAst> = collectGlobalStatements(ast)
    val symbols = ProgramSymbols(functionAsts, globalVars)
    val ctx = ProgramContext(
        currentSource = SourceTracker(), symbols, features, modules
    )
    val functionInstrs: Map<String, List<BigtonInstr>> = functionAsts
        .map { (name, ast) -> name to generateFunction(ast, ctx) }
        .toMap()
    val globalInstrs: List<BigtonInstr>
        = generateGlobal(globalAst, ctx)
    return BigtonProgram(functionInstrs, globalInstrs)
}