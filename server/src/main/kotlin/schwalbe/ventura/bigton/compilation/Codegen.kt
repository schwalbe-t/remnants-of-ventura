
package schwalbe.ventura.bigton.compilation

import schwalbe.ventura.bigton.*
import schwalbe.ventura.bigton.runtime.*

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
    val functions: Map<String, BigtonAst>,
    val globalVars: Set<String>
)

private class LineCounter(var current: Int)

private fun LineCounter.setLine(
    newLine: Int, instrs: MutableList<BigtonInstr>
) {
    if (newLine == this.current) { return }
    this.current = newLine
    instrs.add(BigtonInstr(BigtonInstrType.SOURCE_LINE, newLine))
}

private data class ProgramContext(
    val line: LineCounter,
    val symbols: ProgramSymbols,
    val features: Set<BigtonFeature>,
    val modules: List<BigtonRuntime.Module>
)

private fun ProgramContext.assertFeatureSupported(feature: BigtonFeature) {
    if (feature in this.features) { return }
    throw BigtonException(
        BigtonErrorType.FEATURE_UNSUPPORTED, this.line.current
    )
}

private fun ProgramContext.findFunctionArgc(name: String): Int? {
    val userFun: BigtonAst? = this.symbols.functions[name]
    if (userFun != null) {
        return userFun.castArg<BigtonAstFunction>().argNames.size
    }
    val builtinModule: BigtonRuntime.Module? = this.modules
        .find { m -> name in m.builtinFunctions.keys }
    if (builtinModule != null) {
        return builtinModule.builtinFunctions[name]!!.argc
    }
    return null
}

private data class ScopeContext(
    val inFunction: Boolean,
    val inLoop: Boolean,
    val vars: MutableSet<String>,
    val program: ProgramContext
)

private fun ScopeContext.inChildScope(
    isLoop: Boolean = this.inLoop
) = ScopeContext(
    inFunction = this.inFunction,
    inLoop = isLoop,
    vars = this.vars.toMutableSet(),
    program = this.program
)

private fun ScopeContext.assertVariableExists(name: String) {
    if (name in this.vars || name in this.program.symbols.globalVars) { return }
    throw BigtonException(
        BigtonErrorType.UNKNOWN_VARIABLE, this.program.line.current
    )
}

private fun ScopeContext.assertInFunction() {
    if (this.inFunction) { return }
    throw BigtonException(
        BigtonErrorType.RETURN_OUTSIDE_FUNCTION, this.program.line.current
    )
}

private fun ScopeContext.assertInLoop() {
    if (this.inLoop) { return }
    throw BigtonException(
        BigtonErrorType.LOOP_CONTROLS_OUTSIDE_LOOP, this.program.line.current
    )
}

private fun generateExpression(
    ast: BigtonAst, ctx: ScopeContext, instrs: MutableList<BigtonInstr>
) {
    ctx.program.line.setLine(ast.line, instrs)
    for (child in ast.children) {
        generateExpression(child, ctx, instrs)
    }
    instrs.add(when (ast.type) {
        BigtonAstType.IDENTIFIER -> {
            val name = ast.castArg<String>()
            ctx.assertVariableExists(name)
            BigtonInstr(BigtonInstrType.LOAD_VARIABLE, name)
        }
        BigtonAstType.NULL_LITERAL -> BigtonInstr(
            BigtonInstrType.LOAD_VALUE, BigtonNull
        )
        BigtonAstType.INT_LITERAL -> BigtonInstr(
            BigtonInstrType.LOAD_VALUE,
            BigtonInt(ast.castArg<String>().toLong())
        )
        BigtonAstType.FLOAT_LITERAL -> {
            ctx.program.assertFeatureSupported(BigtonFeature.FPU_MODULE)
            BigtonInstr(
                BigtonInstrType.LOAD_VALUE,
                BigtonFloat(ast.castArg<String>().toDouble())
            )
        }
        BigtonAstType.STRING_LITERAL -> BigtonInstr(
            BigtonInstrType.LOAD_VALUE,
            BigtonString(ast.castArg<String>())
        )
        BigtonAstType.TUPLE_LITERAL -> BigtonInstr(
            BigtonInstrType.LOAD_TUPLE, ast.children.size
        )
        BigtonAstType.OBJECT_LITERAL -> {
            ctx.program.assertFeatureSupported(BigtonFeature.OBJECTS)
            BigtonInstr(
                BigtonInstrType.LOAD_OBJECT, ast.castArg<List<String>>()
            )
        }
        BigtonAstType.CALL -> {
            val name = ast.castArg<String>()
            val argc: Int? = ctx.program.findFunctionArgc(name)
            if (argc == null) {
                throw BigtonException(
                    BigtonErrorType.UNKNOWN_FUNCTION, ast.line
                )
            }
            if (ast.children.size < argc) {
                throw BigtonException(
                    BigtonErrorType.TOO_FEW_CALL_ARGS, ast.line
                )
            }
            if (ast.children.size > argc) {
                throw BigtonException(
                    BigtonErrorType.TOO_MANY_CALL_ARGS, ast.line
                )
            }
            BigtonInstr(BigtonInstrType.CALL, name)
        }
        BigtonAstType.TUPLE_MEMBER -> BigtonInstr(
            BigtonInstrType.LOAD_TUPLE_MEMBER, ast.castArg<Int>()
        )
        BigtonAstType.OBJECT_MEMBER -> {
            ctx.program.assertFeatureSupported(BigtonFeature.OBJECTS)
            BigtonInstr(
                BigtonInstrType.LOAD_OBJECT_MEMBER, ast.castArg<String>()
            )
        }
        BigtonAstType.DEREF -> {
            ctx.program.assertFeatureSupported(BigtonFeature.RAM_MODULE)
            BigtonInstr(BigtonInstrType.LOAD_MEMORY)
        }
        BigtonAstType.NOT -> BigtonInstr(BigtonInstrType.NOT)
        BigtonAstType.NEGATE -> BigtonInstr(BigtonInstrType.NEGATE)
        BigtonAstType.ADD -> BigtonInstr(BigtonInstrType.ADD)
        BigtonAstType.SUBTRACT -> BigtonInstr(BigtonInstrType.SUBTRACT)
        BigtonAstType.MULTIPLY -> BigtonInstr(BigtonInstrType.MULTIPLY)
        BigtonAstType.DIVIDE -> BigtonInstr(BigtonInstrType.DIVIDE)
        BigtonAstType.REMAINDER -> BigtonInstr(BigtonInstrType.REMAINDER)
        BigtonAstType.LESS_THAN -> BigtonInstr(BigtonInstrType.LESS_THAN)
        BigtonAstType.GREATER_THAN -> BigtonInstr(BigtonInstrType.GREATER_THAN)
        BigtonAstType.LESS_THAN_EQUAL -> BigtonInstr(BigtonInstrType.LESS_THAN_EQUAL)
        BigtonAstType.GREATER_THAN_EQUAL -> BigtonInstr(BigtonInstrType.GREATER_THAN_EQUAL)
        BigtonAstType.EQUAL -> BigtonInstr(BigtonInstrType.EQUAL)
        BigtonAstType.NOT_EQUAL -> BigtonInstr(BigtonInstrType.NOT_EQUAL)
        BigtonAstType.AND -> BigtonInstr(BigtonInstrType.AND)
        BigtonAstType.OR -> BigtonInstr(BigtonInstrType.OR)
        else -> throw BigtonException(
            BigtonErrorType.UNHANDLED_AST_TYPE, ast.line
        )
    })
}

private fun generateStatement(
    ast: BigtonAst, ctx: ScopeContext, instrs: MutableList<BigtonInstr>
) {
    ctx.program.line.setLine(ast.line, instrs)
    when (ast.type) {
        BigtonAstType.ASSIGNMENT -> {
            val dest: BigtonAst = ast.children[0]
            val value: BigtonAst = ast.children[1]
            when (dest.type) {
                BigtonAstType.IDENTIFIER -> {
                    val name: String = dest.castArg<String>()
                    ctx.assertVariableExists(name)
                    generateExpression(value, ctx, instrs)
                    instrs.add(BigtonInstr(
                        BigtonInstrType.STORE_EXISTING_VARIABLE, name
                    ))
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
                    BigtonErrorType.ASSIGNMENT_TO_CONST, ast.line
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
            instrs.add(BigtonInstr(BigtonInstrType.STORE_NEW_VARIABLE, name))
        }
        BigtonAstType.FUNCTION -> throw BigtonException(
            BigtonErrorType.FUNCTION_INSIDE_FUNCTION, ast.line
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
    val function: BigtonAstFunction = ast.castArg<BigtonAstFunction>()
    val instrs = mutableListOf<BigtonInstr>()
    // When calling, we push the arguments in normal order onto the stack,
    // then execute the function body.
    // This means that in the function, we need to pop the arguments into
    // variables in the REVERSE order (since the last argument will have
    // been pushed onto the stack by the caller LAST and will therefore be
    // popped next)
    for (argName in function.argNames.asReversed()) {
        instrs.add(BigtonInstr(BigtonInstrType.STORE_NEW_VARIABLE, argName))
    }
    val scope = ScopeContext(
        inFunction = true,
        inLoop = false,
        vars = function.argNames.toMutableSet(),
        program = ctx
    )
    function.body.forEach { n -> generateStatement(n, scope, instrs) }
    return instrs
}

private fun generateGlobal(
    ast: List<BigtonAst>, ctx: ProgramContext
): List<BigtonInstr> {
    val instrs = mutableListOf<BigtonInstr>()
    for (globalName in ctx.symbols.globalVars) {
        instrs.add(BigtonInstr(BigtonInstrType.LOAD_VALUE, BigtonNull))
        instrs.add(BigtonInstr(
            BigtonInstrType.STORE_NEW_VARIABLE, globalName
        ))
    }
    val scope = ScopeContext(
        inFunction = false,
        inLoop = false,
        vars = mutableSetOf(),
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
    val ctx = ProgramContext(LineCounter(0), symbols, features, modules)
    val functionInstrs: Map<String, List<BigtonInstr>> = functionAsts
        .map { (name, ast) -> name to generateFunction(ast, ctx) }
        .toMap()
    val globalInstrs: List<BigtonInstr>
        = generateGlobal(globalAst, ctx)
    return BigtonProgram(functionInstrs, globalInstrs)
}