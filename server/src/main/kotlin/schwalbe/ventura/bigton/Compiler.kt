
package schwalbe.ventura.bigton

data class BigtonSourceFile(val name: String, val content: String)

fun compileSources(
    sources: List<BigtonSourceFile>,
    features: Set<BigtonFeature>,
    modules: List<BigtonModule<*>>,
    builtinFunctions: BigtonBuiltinFunctions<*>
): ByteArray {
    val allSources: List<BigtonSourceFile>
        = builtinFunctions.sourceFiles + sources
    val tokens: List<BigtonToken> = allSources
        .flatMap { (n, c) -> tokenize(n, c) }
    val ast: List<BigtonAst> = BigtonParser(tokens).parseStatementList()
    return generateProgram(ast, features, modules, builtinFunctions)
}