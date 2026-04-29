package schwalbe.ventura.client.game

import schwalbe.ventura.client.Client
import schwalbe.ventura.net.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.uuid.Uuid

val USERCODE_DIR: Path = Path.of("usercode")
    .createDirectories()
    .toAbsolutePath()
val ROBOT_FILES_DIR: Path = USERCODE_DIR
    .resolve(".robots")
    .createDirectories()
    .toAbsolutePath()
const val STORED_SOURCES_REQUEST_INTERVAL: Long = 500

object SourceFiles {

    var lastRequestTimeMs: Long = 0

    fun update(client: Client) {
        val now: Long = System.currentTimeMillis()
        if (now < this.lastRequestTimeMs + STORED_SOURCES_REQUEST_INTERVAL) {
            return
        }
        this.lastRequestTimeMs = now
        client.network.outPackets?.send(Packet.serialize(
            PacketType.REQUEST_STORED_SOURCES, Unit
        ))
    }

    fun uploadModifiedSources(
        client: Client, sources: StoredSourcesInfoPacket
    ) {
        for ((relPath, info) in sources.sources) {
            val path: Path = this.getSourceFile(relPath) ?: continue
            val lastModified: Long = path.getLastModifiedTime().toMillis()
            if (lastModified <= info.lastChangeTimeMs) { continue }
            this.uploadFileContents(client, path)
        }
    }

    fun uploadFileContents(client: Client, path: Path) {
        try {
            val relPath: String = path.relativeTo(USERCODE_DIR).toString()
            val contents: String = Files.readString(path)
            val lastModified: Long = path.getLastModifiedTime().toMillis()
            client.network.outPackets?.send(Packet.serialize(
                PacketType.UPLOAD_SOURCE_CONTENT,
                UploadSourceContentsPacket(relPath, contents, lastModified)
            ))
            println("Uploaded source file '$relPath'")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // does not guarantee that the file exists
    fun getSourceFile(relPath: String): Path? {
        try {
            val path: Path = USERCODE_DIR.resolve(relPath).toAbsolutePath()
            if (!path.exists()) { return null }
            return path
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // guarantees that the file exists
    fun getRobotSourceFile(robotId: Uuid): Path {
        val path: Path = ROBOT_FILES_DIR.resolve("$robotId.bigton")
        if (!path.exists()) {
            Files.writeString(path, "")
        }
        return path
    }

}

fun PacketHandler<Unit>.updateStoredSources(client: Client) = this
    .onPacket(PacketType.STORED_SOURCES) { sources, _ ->
        SourceFiles.uploadModifiedSources(client, sources)
    }
