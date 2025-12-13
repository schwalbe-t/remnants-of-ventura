
package schwalbe.ventura.bigton.compilation

import schwalbe.ventura.bigton.runtime.*

fun compileSource(
    source: String,
    features: Set<BigtonFeature>,
    modules: List<BigtonRuntime.Module>
): BigtonProgram {
    val tokens: List<BigtonToken> = tokenize(source)
    val ast: List<BigtonAst> = BigtonParser(tokens).parseStatementList()
    return emitProgram(ast, features, modules)
}