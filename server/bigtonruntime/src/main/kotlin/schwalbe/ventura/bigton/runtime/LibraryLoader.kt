
package schwalbe.ventura.bigton.runtime

import java.nio.file.Paths

private fun libraryPrefixOfOs(osName: String): String = when {
    "win" in osName -> ""
    "mac" in osName -> "lib"
    "nux" in osName ||
    "nix" in osName -> "lib"
    else -> throw IllegalStateException("unsupported OS")
}

private fun dynlibFileExtOfOs(osName: String): String = when {
    "win" in osName -> "dll"
    "mac" in osName -> "dylib"
    "nux" in osName ||
    "nix" in osName -> "so"
    else -> throw IllegalStateException("unsupported OS")
}

fun loadBigtonRuntime(relDir: String, name: String) {
    val osName: String = System.getProperty("os.name").lowercase()
    val prefix: String = libraryPrefixOfOs(osName)
    val ext: String = dynlibFileExtOfOs(osName)
    val fileName = "${prefix}${name}.${ext}"
    val absPath: String = Paths.get(relDir, fileName)
        .toAbsolutePath().toString()
    System.load(absPath)
}