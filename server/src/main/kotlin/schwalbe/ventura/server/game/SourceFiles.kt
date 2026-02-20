
package schwalbe.ventura.server.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import schwalbe.ventura.MAX_NUM_PLAYER_SOURCE_FILES
import schwalbe.ventura.MAX_SOURCE_FILE_SIZE

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

    fun touch(path: String): Boolean {
        if (path !in this.files.keys) {
            if (this.files.size >= MAX_NUM_PLAYER_SOURCE_FILES) { return false }
            this.files[path] = SourceFile()
        }
        return true
    }

    fun set(path: String, content: String, changeTimeMs: Long): Boolean {
        val contentLenBytes: Int = content.length * 2
        if (contentLenBytes > MAX_SOURCE_FILE_SIZE) { return false }
        val file: SourceFile = this.files[path] ?: return false
        file.content = content
        file.lastChangeTimeMs = changeTimeMs
        return true
    }

    fun getContent(path: String): String
        = this.files[path]?.content ?: ""

    fun getChangeTime(path: String): Long
        = this.files[path]?.lastChangeTimeMs ?: 0L

    fun removeUnused(markUsedFiles: ((String) -> Unit) -> Unit) {
        this.files.values.forEach { it.used = false }
        markUsedFiles { path ->
            val file: SourceFile = this.files[path] ?: return@markUsedFiles
            file.used = true
        }
        this.files.values.removeIf { !it.used }
    }

}