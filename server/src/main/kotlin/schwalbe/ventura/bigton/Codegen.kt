
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
    val shapes: IdBank<List<Int>> = IdBank(),
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
            ctx.program.assertFeatureSupported(BigtonFeature.DYNAMIC_MEMORY)
            val shape: List<Int> = ast.castArg<List<String>>()
                .map { program.strings[it] }
            val shapeId: Int = program.shapes[shape]
            program.instrTypes.add(InstrType.LOAD_OBJECT)
            program.instrArgs.putInt(shapeId)
            program.instrArgs.alignTo(8)
        }
        BigtonAstType.ARRAY_LITERAL -> {
            ctx.program.assertFeatureSupported(BigtonFeature.DYNAMIC_MEMORY)
            program.instrTypes.add(InstrType.LOAD_ARRAY)
            program.instrArgs.putInt(ast.children.size)
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
            val callSiteFileNameId: Int = program
                .strings[ctx.program.currentSource.file]
            program.instrTypes.add(InstrType.SOURCE_FILE)
            program.instrArgs.putInt(callSiteFileNameId)
            program.instrArgs.alignTo(8)
            program.instrTypes.add(InstrType.SOURCE_LINE)
            program.instrArgs.putInt(ctx.program.currentSource.line + 1)
            program.instrArgs.alignTo(8)
        }
        BigtonAstType.TUPLE_MEMBER -> {
            program.instrTypes.add(InstrType.LOAD_TUPLE_MEMBER)
            program.instrArgs.putInt(ast.castArg<Int>())
            program.instrArgs.alignTo(8)
        }
        BigtonAstType.OBJECT_MEMBER -> {
            ctx.program.assertFeatureSupported(BigtonFeature.DYNAMIC_MEMORY)
            val nameId: Int = program.strings[ast.castArg<String>()]
            program.instrTypes.add(InstrType.LOAD_OBJECT_MEMBER)
            program.instrArgs.putInt(nameId)
            program.instrArgs.alignTo(8)
        }
        BigtonAstType.ARRAY_INDEX -> {
            ctx.program.assertFeatureSupported(BigtonFeature.DYNAMIC_MEMORY)
            program.addNoArgInstr(InstrType.LOAD_ARRAY_ELEMENT)
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
    }
}

private fun generateStatement(
    ast: BigtonAst, ctx: ScopeContext, program: ProgramBuilder
) {
    ctx.program.currentSource.setLine(ast.source.line, program)
    ctx.program.currentSource.setFile(ast.source.file, program)
    when (ast.type) {
        BigtonAstType.ASSIGNMENT -> {
            val dest: BigtonAst = ast.children[0]
            val value: BigtonAst = ast.children[1]
            when (dest.type) {
                BigtonAstType.IDENTIFIER -> {
                    generateExpression(value, ctx, program)
                    val name: String = dest.castArg<String>()
                    val localIdx: Int? = ctx.getLocalRelIndex(name)
                    if (localIdx != null) {
                        program.instrTypes.add(InstrType.STORE_LOCAL)
                        program.instrArgs.putInt(localIdx)
                        program.instrArgs.alignTo(8)
                    } else if (name in ctx.program.symbols.globalVars) {
                        val globalId: Int = ctx.program.symbols
                            .globalIds[name]!!
                        program.instrTypes.add(InstrType.STORE_GLOBAL)
                        program.instrArgs.putInt(globalId)
                        program.instrArgs.alignTo(8)
                    } else {
                        throw BigtonException(
                            BigtonErrorType.UNKNOWN_VARIABLE, ast.source
                        )
                    }
                }
                BigtonAstType.OBJECT_MEMBER -> {
                    ctx.program.assertFeatureSupported(
                        BigtonFeature.DYNAMIC_MEMORY
                    )
                    val member: String = dest.castArg<String>()
                    generateExpression(dest.children[0], ctx, program)
                    generateExpression(value, ctx, program)
                    program.instrTypes.add(InstrType.STORE_OBJECT_MEMBER)
                    program.instrArgs.putInt(program.strings[member])
                    program.instrArgs.alignTo(8)
                }
                BigtonAstType.ARRAY_INDEX -> {
                    ctx.program.assertFeatureSupported(
                        BigtonFeature.DYNAMIC_MEMORY
                    )
                    val array: BigtonAst = dest.children[0]
                    val index: BigtonAst = dest.children[1]
                    generateExpression(array, ctx, program)
                    generateExpression(index, ctx, program)
                    generateExpression(value, ctx, program)
                    program.addNoArgInstr(InstrType.STORE_ARRAY_ELEMENT)
                }
                else -> throw BigtonException(
                    BigtonErrorType.ASSIGNMENT_TO_CONST, ast.source
                )
            }
        }
        BigtonAstType.IF -> {
            val cond: BigtonAst = ast.children[0]
            val (ifAst, elseAst) = ast.castArg<
                Pair<List<BigtonAst>, List<BigtonAst>?>
            >()
            generateExpression(cond, ctx, program)
            val ifBody = program.child()
            generateStatementList(ifAst, ctx.inChildScope(), ifBody)
            val elseBody = program.child()
            if (elseAst != null) {
                generateStatementList(elseAst, ctx.inChildScope(), elseBody)
            }
            program.instrTypes.add(InstrType.IF)
            program.instrArgs.beginStruct()
            program.instrArgs.putInt(ifBody.instrTypes.size)
            program.instrArgs.putInt(elseBody.instrTypes.size)
            program.instrArgs.endStruct()
            program.append(ifBody)
            program.append(elseBody)
        }
        BigtonAstType.LOOP -> {
            val bodyAst = ast.castArg<List<BigtonAst>>()
            val body = program.child()
            generateStatementList(
                bodyAst, ctx.inChildScope(isLoop = true), body
            )
            program.instrTypes.add(InstrType.LOOP)
            program.instrArgs.putInt(body.instrTypes.size)
            program.instrArgs.alignTo(8)
            program.append(body)
        }
        BigtonAstType.TICK -> {
            val bodyAst = ast.castArg<List<BigtonAst>>()
            val body = program.child()
            generateStatementList(
                bodyAst, ctx.inChildScope(isLoop = true), body
            )
            program.instrTypes.add(InstrType.TICK)
            program.instrArgs.putInt(body.instrTypes.size)
            program.instrArgs.alignTo(8)
            program.append(body)
        }
        BigtonAstType.WHILE -> {
            val cond: BigtonAst = ast.children[0]
            val bodyAst = ast.castArg<List<BigtonAst>>()
            val childCtx = ctx.inChildScope(isLoop = true)
            // The following BIGTON source:
            //
            //     while x { ... }
            //
            // ...is converted to BIGTON runtime instructions closer matching
            // the following BIGTON source:
            // 
            //     loop {
            //         if instructionsOf(x) {} else { break }
            //         ...
            //     }
            //
            val body = program.child()
            generateExpression(cond, ctx, body)
            body.instrTypes.add(InstrType.IF)
            body.instrArgs.beginStruct()
            body.instrArgs.putInt(0 /* ifBodyLength */) 
            body.instrArgs.putInt(1 /* elseBodyLength */)
            body.instrArgs.endStruct()
            body.addNoArgInstr(InstrType.BREAK)
            generateStatementList(bodyAst, childCtx, body)
            program.instrTypes.add(InstrType.LOOP)
            program.instrArgs.putInt(body.instrTypes.size)
            program.instrArgs.alignTo(8)
            program.append(body)
        }
        BigtonAstType.CONTINUE -> {
            ctx.assertInLoop()
            program.addNoArgInstr(InstrType.CONTINUE)
        }
        BigtonAstType.BREAK -> {
            ctx.assertInLoop()
            program.addNoArgInstr(InstrType.BREAK)
        }
        BigtonAstType.RETURN -> {
            ctx.assertInFunction()
            val value: BigtonAst = ast.children[0]
            generateExpression(value, ctx, program)
            program.addNoArgInstr(InstrType.RETURN)
        }
        BigtonAstType.VARIABLE -> {
            val name: String = ast.castArg<String>()
            val value: BigtonAst = ast.children[0]
            generateExpression(value, ctx, program)
            if (ctx.inGlobal) {
                val globalId: Int = ctx.program.symbols.globalIds[name]!!
                program.instrTypes.add(InstrType.STORE_GLOBAL)
                program.instrArgs.putInt(globalId)
                program.instrArgs.alignTo(8)
            } else {
                val idx: Int = ctx.numLocals
                ctx.locals[name] = idx
                ctx.numLocals += 1
                program.addNoArgInstr(InstrType.PUSH_LOCAL)
            }
        }
        BigtonAstType.FUNCTION -> throw BigtonException(
            BigtonErrorType.FUNCTION_INSIDE_FUNCTION, ast.source
        )
        else -> {
            generateExpression(ast, ctx, program)
            program.addNoArgInstr(InstrType.DISCARD)
        }
    }
}

private fun generateStatementList(
    ast: List<BigtonAst>, ctx: ScopeContext, program: ProgramBuilder
) {
    ast.forEach { n -> generateStatement(n, ctx, program) }
}

private fun generateFunction(
    ast: BigtonAst, ctx: ProgramContext, program: ProgramBuilder
) {
    ctx.assertFeatureSupported(BigtonFeature.CUSTOM_FUNCTIONS)
    ctx.currentSource.reset()
    val function: BigtonAstFunction = ast.castArg<BigtonAstFunction>()
    // When calling, we push the arguments in normal order onto the stack,
    // then execute the function body.
    // This means that in the function, we need to pop the arguments into
    // variables in the REVERSE order (since the last argument will have
    // been pushed onto the stack by the caller LAST and will therefore be
    // popped next)
    val params: MutableMap<String, Int> = mutableMapOf()
    for ((i, n) in function.argNames.asReversed().withIndex()) {
        program.addNoArgInstr(InstrType.PUSH_LOCAL)
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
    generateStatementList(function.body, scope, program)
}

private fun generateGlobal(
    ast: List<BigtonAst>, ctx: ProgramContext, program: ProgramBuilder
) {
    ctx.currentSource.reset()
    for (globalId in ctx.symbols.globalIds.values) {
        program.addNoArgInstr(InstrType.LOAD_NULL)
        program.instrTypes.add(InstrType.STORE_GLOBAL)
        program.instrArgs.putInt(globalId)
        program.instrArgs.alignTo(8)
    }
    val scope = ScopeContext(
        inGlobal = true,
        inFunction = false,
        inLoop = false,
        locals = mutableMapOf(),
        numLocals = 0,
        program = ctx
    )
    generateStatementList(ast, scope, program)
}

private data class IrFunctionInfo(
    val name: Int,
    val declFile: Int, val declLine: Int,
    val start: Int, val length: Int
)

private data class IrBuiltinFunctionInfo(
    val name: Int,
    val cost: Int
)

fun generateProgram(
    ast: List<BigtonAst>,
    features: Set<BigtonFeature>,
    modules: List<BigtonModule>,
    builtinFunctions: BigtonBuiltinFunctions = BigtonModules.functions
): ByteArray {
    val functionAsts: Map<String, BigtonAst> = collectFunctions(ast)
    val functionIds: Map<String, Int> = functionAsts.keys.withIndex()
        .associateBy({ it.value }, { it.index })
    val globalVars: Set<String> = collectGlobalVars(ast)
    val globalIds: Map<String, Int> = globalVars.withIndex()
        .associateBy({ it.value }, { it.index })
    val symbols = ProgramSymbols(
        builtinFunctions,
        functionAsts, functionIds,
        globalVars, globalIds
    )
    val ctx = ProgramContext(
        currentSource = SourceTracker(), symbols, features, modules
    )
    val program = ProgramBuilder()
    val irFunctions: List<IrFunctionInfo> = functionIds.map { (name, id) ->
        val ast: BigtonAst = functionAsts[name]!!
        val func: BigtonAstFunction = ast.castArg<BigtonAstFunction>()
        val funcBody = program.child()
        generateFunction(ast, ctx, funcBody)
        val start: Int = program.instrTypes.size
        val length: Int = funcBody.instrTypes.size
        program.append(funcBody)
        IrFunctionInfo(
            name = program.strings[func.name],
            declFile = program.strings[ast.source.file],
            declLine = ast.source.line + 1,
            start, length
        )
    }
    val irBuiltinFunctions: List<IrBuiltinFunctionInfo>
        = builtinFunctions.functions
        .map { IrBuiltinFunctionInfo(program.strings[it.name], it.cost) }
    val globalAst: List<BigtonAst> = collectGlobalStatements(ast)
    val globalStart: Int = program.instrTypes.size
    generateGlobal(globalAst, ctx, program)
    val globalEnd: Int = program.instrTypes.size
    val unknownStringId: Int = program.strings["<unknown>"]
    val out = BinaryWriter()
    // --- bigton_program_t header ---
    out.beginStruct()
    // bigton_instr_idx_t numInstrs
    out.putInt(program.instrTypes.size)
    // bigton_str_id_t numStrings
    out.putInt(program.strings.values.size)
    // bigton_shape_id_t numShapes
    out.putInt(program.shapes.values.size)
    // bigton_slot_t numFunctions
    out.putInt(irFunctions.size)
    // bigton_slot_t numBuiltinFunctions
    out.putInt(irBuiltinFunctions.size)
    // bigton_slot_t numGlobalVars
    out.putInt(globalIds.size)
    // uint32_t numShapeProps
    val numShapeProps: Int = program.shapes.values.sumOf { it.size }
    out.putInt(numShapeProps)
    // uint64_t numConstStringChars
    val numConstStringChars: Int = program.strings.values.sumOf { it.length }
    out.putLong(numConstStringChars.toLong())
    // bigton_str_id_t unknownStrId
    out.putInt(unknownStringId)
    // bigton_instr_idx_t globalStart
    out.putInt(globalStart)
    // bigton_instr_idx_t globalLength
    out.putInt(globalEnd - globalStart)
    out.endStruct()
    // --- bigton_instr_args_t instrArgs[header.numInstrs] ---
    out.putBytes(program.instrArgs.output.toByteArray())
    // --- bigton_const_string_t constStrings[header.numStrings] ---
    var currStrOffset = 0L
    for (constString in program.strings.values) {
        val charLength: Long = constString.length.toLong()
        out.beginStruct()           // bigton_const_string_t
        out.putLong(currStrOffset)  // uint64_t firstOffset
        out.putLong(charLength)     // uint64_t charLength
        out.endStruct()
        currStrOffset += charLength
    }
    // --- bigton_shape_t shapes[header.numShapes] ---
    var currPropOffset = 0
    for (shape in program.shapes.values) {
        out.beginStruct()           // bigton_shape_t
        out.putInt(shape.size)      // uint32_t propCount
        out.putInt(currPropOffset)  // uint32_t firstPropOffset
        out.endStruct()
        currPropOffset += shape.size
    }
    // --- bigton_function_t functions[header.numFunctions] ---
    for (f in irFunctions) {
        out.beginStruct()           // bigton_function_t
        out.putInt(f.name)          // bigton_str_id_t name
        out.beginStruct()           // bigton_source_t declSource
        out.putInt(f.declFile)      //     bigton_str_id_t file
        out.putInt(f.declLine)      //     uint32_t line
        out.endStruct()
        out.putInt(f.start)         // bigton_instr_idx_t start
        out.putInt(f.length)        // bigton_instr_idx_t length
        out.endStruct()
    }
    // --- bigton_builtin_function_t builtinFunctions[header.numBuiltinFunctions] ---
    for (f in irBuiltinFunctions) {
        out.beginStruct()           // bigton_builtin_function_t
        out.putInt(f.name)          // bigton_str_id_t name
        out.putInt(f.cost)          // uint32_t cost
        out.endStruct()
    }
    // --- bigton_shape_prop_t shapeProps[header.numShapeProps] ---
    for (shape in program.shapes.values) {
        for (propName in shape) {
            out.beginStruct()       // bigton_shape_prop_t
            out.putInt(propName)    // bigton_str_id_t name
            out.endStruct()
        }
    }
    // --- bigton_char_t constStringChars[header.numConstStringChars] ---
    for (constString in program.strings.values) {
        out.putString(constString)
    }
    // --- bigton_instr_type_t instrTypes[header.numInstrs] ---
    for (instrType in program.instrTypes) {
        out.putByte(instrType.ordinal.toByte())
    }
    return out.output.toByteArray()
}