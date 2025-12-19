
package schwalbe.ventura.bigton.compilation

import schwalbe.ventura.bigton.*
import schwalbe.ventura.bigton.runtime.*

data class BigtonSourceFile(val name: String, val content: String)

fun compileSources(
    sources: List<BigtonSourceFile>,
    features: Set<BigtonFeature>,
    modules: List<BigtonRuntime.Module>
): BigtonProgram {
    val tokens: List<BigtonToken> = sources
        .map { (n, c) -> tokenize(n, c) }
        .flatten()
    val ast: List<BigtonAst> = BigtonParser(tokens).parseStatementList()
    return generateProgram(ast, features, modules)
}