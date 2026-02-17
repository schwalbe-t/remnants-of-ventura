
package schwalbe.ventura.server.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class SourceFiles {

    @Serializable
    class SourceFile(
        var content: String,
        @Transient
        var used: Boolean = false
    )

    private val files: MutableMap<String, SourceFile> = mutableMapOf()

    operator fun set(path: String, content: String) {
        val file: SourceFile = this.files.getOrPut(path) { SourceFile(content) }
        file.content = content
    }

    operator fun get(path: String): String = this.files[path]?.content ?: ""

    fun removeUnused(markUsedFiles: ((String) -> Unit) -> Unit) {
        this.files.values.forEach { it.used = false }
        markUsedFiles { path ->
            val file: SourceFile = this.files[path] ?: return@markUsedFiles
            file.used = true
        }
        this.files.values.removeIf { !it.used }
    }

}