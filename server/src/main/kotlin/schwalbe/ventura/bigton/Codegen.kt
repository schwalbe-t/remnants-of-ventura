
// package schwalbe.ventura.bigton

// private fun collectFunctions(ast: List<BigtonAst>): Map<String, BigtonAst>
//     = ast
//     .filter { n -> n.type == BigtonAstType.FUNCTION }
//     .map { f -> f.castArg<BigtonAstFunction>().name to f }
//     .toMap()

// private fun collectGlobalVars(ast: List<BigtonAst>): Set<String>
//     = ast
//     .filter { n -> n.type == BigtonAstType.VARIABLE }
//     .map { v -> v.castArg<String>() }
//     .toSet()

// private fun collectGlobalStatements(ast: List<BigtonAst>): List<BigtonAst>
//     = ast
//     .filter { n -> n.type != BigtonAstType.FUNCTION }
//     .toList()

// private data class ProgramSymbols(
//     val functions: Map<String, BigtonAst>,
//     val globalVars: Set<String>
// )

// private class SourceTracker(
//     var line: Int = -1,
//     var file: String = "<unknown>"
// )

// private fun SourceTracker.reset() {
//     this.line = -1
//     this.file = "<unknown>"
// }

// private fun SourceTracker.setLine(
//     newLine: Int, instrs: MutableList<BigtonInstr>
// ) {
//     if (newLine == this.line) { return }
//     this.line = newLine
//     instrs.add(BigtonInstr(BigtonInstrType.SOURCE_LINE, newLine + 1))
// }

// private fun SourceTracker.setFile(
//     newFile: String, instrs: MutableList<BigtonInstr>
// ) {
//     if (newFile == this.file) { return }
//     this.file = newFile
//     instrs.add(BigtonInstr(BigtonInstrType.SOURCE_FILE, newFile))
// }

// private fun SourceTracker.toSource(): BigtonSource
//     = BigtonSource(this.line, this.file)

// private data class ProgramContext(
//     val currentSource: SourceTracker,
//     val symbols: ProgramSymbols,
//     val features: Set<BigtonFeature>,
//     val modules: List<BigtonRuntime.Module>
// )

// private fun ProgramContext.assertFeatureSupported(feature: BigtonFeature) {
//     if (feature in this.features) { return }
//     throw BigtonException(
//         BigtonErrorType.FEATURE_UNSUPPORTED, this.currentSource.toSource()
//     )
// }

// private fun ProgramContext.findFunctionArgc(name: String): Int? {
//     val userFun: BigtonAst? = this.symbols.functions[name]
//     if (userFun != null) {
//         return userFun.castArg<BigtonAstFunction>().argNames.size
//     }
//     val builtinModule: BigtonRuntime.Module? = this.modules
//         .find { m -> name in m.builtinFunctions.keys }
//     if (builtinModule != null) {
//         return builtinModule.builtinFunctions[name]!!.argc
//     }
//     return null
// }

// // var a [0] (numLocals = 1)
// // a -> -numLocals + 0 -> -1

// // var b [1] (numLocals = 2)
// // a -> -numLocals + 0 -> -2
// // b -> -numLocals + 1 -> -1

// // var c [2] (numLocals = 3)
// // a -> -numLocals + 0 -> -3
// // b -> -numLocals + 1 -> -2
// // c -> -numLocals + 2 -> -1 

// private class ScopeContext(
//     val inGlobal: Boolean,
//     val inFunction: Boolean,
//     val inLoop: Boolean,
//     val locals: MutableMap<String, Int>,
//     var numLocals: Int,
//     val program: ProgramContext
// )

// private fun ScopeContext.inChildScope(
//     isLoop: Boolean = this.inLoop
// ) = ScopeContext(
//     inGlobal = false,
//     inFunction = this.inFunction,
//     inLoop = isLoop,
//     locals = this.locals.toMutableMap(),
//     numLocals = this.numLocals,
//     program = this.program
// )

// private fun ScopeContext.getLocalRelIndex(name: String): Int? {
//     val idx: Int? = this.locals[name]
//     if (idx == null) { return idx }
//     return this.numLocals - 1 - idx
// }

// private fun ScopeContext.assertInFunction() {
//     if (this.inFunction) { return }
//     throw BigtonException(
//         BigtonErrorType.RETURN_OUTSIDE_FUNCTION,
//         this.program.currentSource.toSource()
//     )
// }

// private fun ScopeContext.assertInLoop() {
//     if (this.inLoop) { return }
//     throw BigtonException(
//         BigtonErrorType.LOOP_CONTROLS_OUTSIDE_LOOP,
//         this.program.currentSource.toSource()
//     )
// }

// private fun generateExpression(
//     ast: BigtonAst, ctx: ScopeContext, instrs: MutableList<BigtonInstr>
// ) {
//     ctx.program.currentSource.setLine(ast.source.line, instrs)
//     ctx.program.currentSource.setFile(ast.source.file, instrs)
//     for (child in ast.children) {
//         generateExpression(child, ctx, instrs)
//     }
//     instrs.add(when (ast.type) {
//         BigtonAstType.IDENTIFIER -> {
//             val name = ast.castArg<String>()
//             val localIdx: Int? = ctx.getLocalRelIndex(name)
//             if (localIdx != null) {
//                 BigtonInstr(BigtonInstrType.LOAD_LOCAL, localIdx)
//             } else if (name in ctx.program.symbols.globalVars) {
//                 BigtonInstr(BigtonInstrType.LOAD_GLOBAL, name)
//             } else {
//                 throw BigtonException(
//                     BigtonErrorType.UNKNOWN_VARIABLE, ast.source
//                 )
//             }
//         }
//         BigtonAstType.NULL_LITERAL -> BigtonInstr(
//             BigtonInstrType.LOAD_VALUE, BigtonNull
//         )
//         BigtonAstType.INT_LITERAL -> BigtonInstr(
//             BigtonInstrType.LOAD_VALUE,
//             BigtonInt(ast.castArg<String>().toLong())
//         )
//         BigtonAstType.FLOAT_LITERAL -> {
//             ctx.program.assertFeatureSupported(BigtonFeature.FPU_MODULE)
//             BigtonInstr(
//                 BigtonInstrType.LOAD_VALUE,
//                 BigtonFloat(ast.castArg<String>().toDouble())
//             )
//         }
//         BigtonAstType.STRING_LITERAL -> BigtonInstr(
//             BigtonInstrType.LOAD_VALUE,
//             BigtonString(ast.castArg<String>())
//         )
//         BigtonAstType.TUPLE_LITERAL -> BigtonInstr(
//             BigtonInstrType.LOAD_TUPLE, ast.children.size
//         )
//         BigtonAstType.OBJECT_LITERAL -> {
//             ctx.program.assertFeatureSupported(BigtonFeature.OBJECTS)
//             BigtonInstr(
//                 BigtonInstrType.LOAD_OBJECT, ast.castArg<List<String>>()
//             )
//         }
//         BigtonAstType.CALL -> {
//             val name = ast.castArg<String>()
//             val argc: Int? = ctx.program.findFunctionArgc(name)
//             if (argc == null) {
//                 throw BigtonException(
//                     BigtonErrorType.UNKNOWN_FUNCTION, ast.source
//                 )
//             }
//             if (ast.children.size < argc) {
//                 throw BigtonException(
//                     BigtonErrorType.TOO_FEW_CALL_ARGS, ast.source
//                 )
//             }
//             if (ast.children.size > argc) {
//                 throw BigtonException(
//                     BigtonErrorType.TOO_MANY_CALL_ARGS, ast.source
//                 )
//             }
//             BigtonInstr(BigtonInstrType.CALL, name)
//         }
//         BigtonAstType.TUPLE_MEMBER -> BigtonInstr(
//             BigtonInstrType.LOAD_TUPLE_MEMBER, ast.castArg<Int>()
//         )
//         BigtonAstType.OBJECT_MEMBER -> {
//             ctx.program.assertFeatureSupported(BigtonFeature.OBJECTS)
//             BigtonInstr(
//                 BigtonInstrType.LOAD_OBJECT_MEMBER, ast.castArg<String>()
//             )
//         }
//         BigtonAstType.DEREF -> {
//             ctx.program.assertFeatureSupported(BigtonFeature.RAM_MODULE)
//             BigtonInstr(BigtonInstrType.LOAD_MEMORY)
//         }
//         BigtonAstType.NOT -> BigtonInstr(BigtonInstrType.NOT)
//         BigtonAstType.NEGATE -> BigtonInstr(BigtonInstrType.NEGATE)
//         BigtonAstType.ADD -> BigtonInstr(BigtonInstrType.ADD)
//         BigtonAstType.SUBTRACT -> BigtonInstr(BigtonInstrType.SUBTRACT)
//         BigtonAstType.MULTIPLY -> BigtonInstr(BigtonInstrType.MULTIPLY)
//         BigtonAstType.DIVIDE -> BigtonInstr(BigtonInstrType.DIVIDE)
//         BigtonAstType.REMAINDER -> BigtonInstr(BigtonInstrType.REMAINDER)
//         BigtonAstType.LESS_THAN -> BigtonInstr(BigtonInstrType.LESS_THAN)
//         BigtonAstType.GREATER_THAN -> BigtonInstr(BigtonInstrType.GREATER_THAN)
//         BigtonAstType.LESS_THAN_EQUAL -> BigtonInstr(BigtonInstrType.LESS_THAN_EQUAL)
//         BigtonAstType.GREATER_THAN_EQUAL -> BigtonInstr(BigtonInstrType.GREATER_THAN_EQUAL)
//         BigtonAstType.EQUAL -> BigtonInstr(BigtonInstrType.EQUAL)
//         BigtonAstType.NOT_EQUAL -> BigtonInstr(BigtonInstrType.NOT_EQUAL)
//         BigtonAstType.AND -> BigtonInstr(BigtonInstrType.AND)
//         BigtonAstType.OR -> BigtonInstr(BigtonInstrType.OR)
//         else -> throw BigtonException(
//             BigtonErrorType.UNHANDLED_AST_TYPE, ast.source
//         )
//     })
// }

// private fun generateStatement(
//     ast: BigtonAst, ctx: ScopeContext, instrs: MutableList<BigtonInstr>
// ) {
//     ctx.program.currentSource.setLine(ast.source.line, instrs)
//     ctx.program.currentSource.setFile(ast.source.file, instrs)
//     when (ast.type) {
//         BigtonAstType.ASSIGNMENT -> {
//             val dest: BigtonAst = ast.children[0]
//             val value: BigtonAst = ast.children[1]
//             when (dest.type) {
//                 BigtonAstType.IDENTIFIER -> {
//                     generateExpression(value, ctx, instrs)
//                     val name: String = dest.castArg<String>()
//                     val localIdx: Int? = ctx.getLocalRelIndex(name)
//                     instrs.add(if (localIdx != null) {
//                         BigtonInstr(BigtonInstrType.STORE_LOCAL, localIdx)
//                     } else if (name in ctx.program.symbols.globalVars) {
//                         BigtonInstr(BigtonInstrType.STORE_GLOBAL, name)
//                     } else {
//                         throw BigtonException(
//                             BigtonErrorType.UNKNOWN_VARIABLE, ast.source
//                         )
//                     })
//                 }
//                 BigtonAstType.DEREF -> {
//                     ctx.program.assertFeatureSupported(BigtonFeature.RAM_MODULE)
//                     generateExpression(dest.children[0], ctx, instrs)
//                     generateExpression(value, ctx, instrs)
//                     instrs.add(BigtonInstr(BigtonInstrType.STORE_MEMORY))
//                 }
//                 BigtonAstType.OBJECT_MEMBER -> {
//                     ctx.program.assertFeatureSupported(BigtonFeature.OBJECTS)
//                     val member: String = dest.castArg<String>()
//                     generateExpression(dest.children[0], ctx, instrs)
//                     generateExpression(value, ctx, instrs)
//                     instrs.add(BigtonInstr(
//                         BigtonInstrType.STORE_OBJECT_MEMBER, member
//                     ))
//                 }
//                 else -> throw BigtonException(
//                     BigtonErrorType.ASSIGNMENT_TO_CONST, ast.source
//                 )
//             }
//         }
//         BigtonAstType.IF -> {
//             val cond: BigtonAst = ast.children[0]
//             val (if_ast, else_ast) = ast.castArg<
//                 Pair<List<BigtonAst>, List<BigtonAst>?>
//             >()
//             generateExpression(cond, ctx, instrs)
//             val if_body: List<BigtonInstr>
//                 = generateStatementList(if_ast, ctx.inChildScope())
//             val else_body: List<BigtonInstr>? =
//                 if (else_ast == null) { null }
//                 else { generateStatementList(else_ast, ctx.inChildScope()) }
//             instrs.add(BigtonInstr(
//                 BigtonInstrType.IF, Pair(if_body, else_body)
//             ))
//         }
//         BigtonAstType.LOOP -> {
//             val body_ast = ast.castArg<List<BigtonAst>>()
//             val body: List<BigtonInstr> = generateStatementList(
//                 body_ast, ctx.inChildScope(isLoop = true)
//             )
//             instrs.add(BigtonInstr(BigtonInstrType.LOOP, body))
//         }
//         BigtonAstType.TICK -> {
//             val body_ast = ast.castArg<List<BigtonAst>>()
//             val body: List<BigtonInstr> = generateStatementList(
//                 body_ast, ctx.inChildScope(isLoop = true)
//             )
//             instrs.add(BigtonInstr(BigtonInstrType.TICK, body))
//         }
//         BigtonAstType.WHILE -> {
//             val cond: BigtonAst = ast.children[0]
//             val body_ast = ast.castArg<List<BigtonAst>>()
//             // The following BIGTON source...:
//             //
//             //     while x { ... }
//             //
//             // ...is converted to the following BIGTON runtime instructions:
//             // 
//             //     LOOP {
//             //         instructionsOf(x)
//             //         NOT
//             //         IF { BREAK }
//             //         ...
//             //     }
//             //
//             val body: MutableList<BigtonInstr> = mutableListOf()
//             generateExpression(cond, ctx, body)
//             body.add(BigtonInstr(BigtonInstrType.NOT))
//             body.add(BigtonInstr(
//                 BigtonInstrType.IF,
//                 Pair<List<BigtonInstr>, List<BigtonInstr>?>(
//                     listOf(BigtonInstr(BigtonInstrType.BREAK)),
//                     null
//                 )
//             ))
//             body.addAll(generateStatementList(
//                 body_ast, ctx.inChildScope(isLoop = true)
//             ))
//             instrs.add(BigtonInstr(BigtonInstrType.LOOP, body))
//         }
//         BigtonAstType.CONTINUE -> {
//             ctx.assertInLoop()
//             instrs.add(BigtonInstr(BigtonInstrType.CONTINUE))
//         }
//         BigtonAstType.BREAK -> {
//             ctx.assertInLoop()
//             instrs.add(BigtonInstr(BigtonInstrType.BREAK))
//         }
//         BigtonAstType.RETURN -> {
//             ctx.assertInFunction()
//             val value: BigtonAst = ast.children[0]
//             generateExpression(value, ctx, instrs)
//             instrs.add(BigtonInstr(BigtonInstrType.RETURN))
//         }
//         BigtonAstType.VARIABLE -> {
//             val name: String = ast.castArg<String>()
//             val value: BigtonAst = ast.children[0]
//             generateExpression(value, ctx, instrs)
//             instrs.add(if (ctx.inGlobal) {
//                 BigtonInstr(BigtonInstrType.STORE_GLOBAL, name)
//             } else {
//                 val idx: Int = ctx.numLocals
//                 ctx.locals[name] = idx
//                 ctx.numLocals += 1
//                 BigtonInstr(BigtonInstrType.PUSH_LOCAL)
//             })
//         }
//         BigtonAstType.FUNCTION -> throw BigtonException(
//             BigtonErrorType.FUNCTION_INSIDE_FUNCTION, ast.source
//         )
//         else -> {
//             generateExpression(ast, ctx, instrs)
//             instrs.add(BigtonInstr(BigtonInstrType.DISCARD))
//         }
//     }
// }

// private fun generateStatementList(
//     ast: List<BigtonAst>, ctx: ScopeContext
// ): List<BigtonInstr> {
//     val instrs = mutableListOf<BigtonInstr>()
//     ast.forEach { n -> generateStatement(n, ctx, instrs) }
//     return instrs
// }

// private fun generateFunction(
//     ast: BigtonAst, ctx: ProgramContext
// ): List<BigtonInstr> {
//     ctx.assertFeatureSupported(BigtonFeature.CUSTOM_FUNCTIONS)
//     ctx.currentSource.reset()
//     val function: BigtonAstFunction = ast.castArg<BigtonAstFunction>()
//     val instrs = mutableListOf<BigtonInstr>()
//     // When calling, we push the arguments in normal order onto the stack,
//     // then execute the function body.
//     // This means that in the function, we need to pop the arguments into
//     // variables in the REVERSE order (since the last argument will have
//     // been pushed onto the stack by the caller LAST and will therefore be
//     // popped next)
//     val params: MutableMap<String, Int> = mutableMapOf()
//     for ((i, n) in function.argNames.asReversed().withIndex()) {
//         instrs.add(BigtonInstr(BigtonInstrType.PUSH_LOCAL))
//         params[n] = i
//     }
//     val scope = ScopeContext(
//         inGlobal = false,
//         inFunction = true,
//         inLoop = false,
//         locals = params,
//         numLocals = function.argNames.size,
//         program = ctx
//     )
//     function.body.forEach { n -> generateStatement(n, scope, instrs) }
//     return instrs
// }

// private fun generateGlobal(
//     ast: List<BigtonAst>, ctx: ProgramContext
// ): List<BigtonInstr> {
//     ctx.currentSource.reset()
//     val instrs = mutableListOf<BigtonInstr>()
//     for (globalName in ctx.symbols.globalVars) {
//         instrs.add(BigtonInstr(BigtonInstrType.LOAD_VALUE, BigtonNull))
//         instrs.add(BigtonInstr(BigtonInstrType.STORE_GLOBAL, globalName))
//     }
//     val scope = ScopeContext(
//         inGlobal = true,
//         inFunction = false,
//         inLoop = false,
//         locals = mutableMapOf(),
//         numLocals = 0,
//         program = ctx
//     )
//     ast.forEach { n -> generateStatement(n, scope, instrs) }
//     return instrs
// }

// fun generateProgram(
//     ast: List<BigtonAst>,
//     features: Set<BigtonFeature>,
//     modules: List<BigtonRuntime.Module>
// ): BigtonProgram {
//     val functionAsts: Map<String, BigtonAst> = collectFunctions(ast)
//     val globalVars: Set<String> = collectGlobalVars(ast)
//     val globalAst: List<BigtonAst> = collectGlobalStatements(ast)
//     val symbols = ProgramSymbols(functionAsts, globalVars)
//     val ctx = ProgramContext(
//         currentSource = SourceTracker(), symbols, features, modules
//     )
//     val functionInstrs: Map<String, List<BigtonInstr>> = functionAsts
//         .map { (name, ast) -> name to generateFunction(ast, ctx) }
//         .toMap()
//     val globalInstrs: List<BigtonInstr>
//         = generateGlobal(globalAst, ctx)
//     return BigtonProgram(functionInstrs, globalInstrs)
// }