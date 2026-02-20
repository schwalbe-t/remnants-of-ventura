package schwalbe.ventura.client.game

import schwalbe.ventura.client.Client
import schwalbe.ventura.net.*
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime

const val USERCODE_DIR: String = "usercode"
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
            val path: Path
            try {
                path = Path.of(USERCODE_DIR, relPath).toAbsolutePath()
            } catch (e: Exception) {
                e.printStackTrace()
                continue
            }
            if (!path.exists()) { continue }
            val lastModified: Long = path.getLastModifiedTime().toMillis()
            if (lastModified <= info.lastChangeTimeMs) { continue }
            val contents: String
            try {
                contents = path.toFile().readText()
            } catch (e: Exception) {
                e.printStackTrace()
                continue
            }
            client.network.outPackets?.send(Packet.serialize(
                PacketType.UPLOAD_SOURCE_CONTENT,
                UploadSourceContentsPacket(
                    path = relPath, contents, lastModified
                )
            ))
            println("Uploaded source file '$relPath'")
        }
    }

}

fun PacketHandler<Unit>.updateStoredSources(client: Client) = this
    .onPacket(PacketType.STORED_SOURCES) { sources, _ ->
        SourceFiles.uploadModifiedSources(client, sources)
    }
