
package schwalbe.ventura.server.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.collections.getOrPut

@Serializable
class SourceFiles {

    @Serializable
    class SourceFile(
        var content: String = "",
        var lastChangeTimeMs: Long = 0,
        @Transient
        var used: Boolean = false
    )

    private val files: MutableMap<String, SourceFile> = mutableMapOf()
    val paths: Set<String> = this.files.keys

    fun touch(path: String) {
        if (path !in this.files.keys) {
            this.files[path] = SourceFile()
        }
    }

    fun set(path: String, content: String, changeTimeMs: Long) {
        val file: SourceFile = this.files.getOrPut(path) { SourceFile(content) }
        file.content = content
        file.lastChangeTimeMs = changeTimeMs
    }

    fun getContent(path: String): String = this.files
        .getOrPut(path, ::SourceFile)
        .content

    fun getChangeTime(path: String): Long = this.files
        .getOrPut(path, ::SourceFile)
        .lastChangeTimeMs

    fun removeUnused(markUsedFiles: ((String) -> Unit) -> Unit) {
        this.files.values.forEach { it.used = false }
        markUsedFiles { path ->
            val file: SourceFile = this.files[path] ?: return@markUsedFiles
            file.used = true
        }
        this.files.values.removeIf { !it.used }
    }

}