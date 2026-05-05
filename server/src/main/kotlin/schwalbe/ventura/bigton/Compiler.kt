
package schwalbe.ventura.bigton

data class BigtonSourceFile(
    val name: String,
    val content: String,
    val isUnrestricted: Boolean = false
)

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
    val unrestricted: Set<String> = allSources.mapNotNullTo(mutableSetOf()) {
        if (it.isUnrestricted) it.name else null
    }
    return generateProgram(
        ast, features, unrestricted, modules, builtinFunctions
    )
}