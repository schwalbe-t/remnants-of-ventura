
package schwalbe.ventura.editor

import kotlinx.serialization.json.Json
import schwalbe.ventura.data.*
import java.nio.file.Files
import java.nio.file.Path


class MutableWorld(
    val chunks: MutableMap<ChunkRef, MutableChunkData>
) {
    companion object
}

fun SerializedWorld.toMutableWorld() = MutableWorld(
    chunks = this.chunks
        .mapValuesTo(mutableMapOf()) { (_, c) -> c.toMutableChunkData() }
)

fun MutableWorld.toSerializedWorld() = SerializedWorld(
    chunks = this.chunks.mapValues { (_, c) -> c.toChunkData() }
)


class MutableChunkData(
    val instances: MutableList<ObjectInstance>
) {
    companion object
}

fun ChunkData.toMutableChunkData() = MutableChunkData(
    instances = this.instances.toMutableList()
)

fun MutableChunkData.toChunkData() = ChunkData(
    instances = this.instances
)


fun MutableWorld.Companion.readFromFile(path: Path): MutableWorld {
    val jsonWorld: String = Files.readString(path)
    val serWorld = Json.decodeFromString<SerializedWorld>(jsonWorld)
    return serWorld.toMutableWorld()
}

fun MutableWorld.writeToFile(path: Path) {
    val serWorld: SerializedWorld = this.toSerializedWorld()
    val jsonWorld: String = Json.encodeToString(serWorld)
    Files.writeString(path, jsonWorld)
}
