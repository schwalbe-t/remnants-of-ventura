
package schwalbe.ventura.bigton.compilation

import schwalbe.ventura.bigton.*
import schwalbe.ventura.bigton.runtime.*

private fun collectFunctions(ast: List<BigtonAst>): Map<String, BigtonAst>
    = ast
    .filter { n -> n.type == BigtonAstType.Function }
    .map { f -> f.castArg<BigtonAstFunction>().name to f }
    .toMap()

private fun collectGlobalStatements(ast: List<BigtonAst>): List<BigtonAst>
    = ast
    .filter { n -> n.type != BigtonAstType.Function }
    .toList()

fun emitProgram(
    ast: List<BigtonInstr>,
    features: Set<BigtonFeature>,
    modules: List<BigtonRuntime.Module>
): BigtonProgram {
    val functionAsts: Map<String, BigtonAst> = collectFunctions(ast)
    val globalAst: List<BigtonAst> = collectGlobalStatements(ast)
    // TODO! emit functions
    // TODO! emit global
    // TODO! return program
}