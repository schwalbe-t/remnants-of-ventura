
package schwalbe.ventura.editor

import schwalbe.ventura.data.*
import java.nio.file.Files
import java.nio.file.Path


class MutableWorld(
    val groundColor: String,
    val chunks: MutableMap<ChunkRef, MutableChunkData> = mutableMapOf(),
) {
    companion object
}

fun SerializedWorld.toMutableWorld() = MutableWorld(
    groundColor = this.groundColor,
    chunks = this.chunks
        .mapValuesTo(mutableMapOf()) { (_, c) -> c.toMutableChunkData() }
)

fun MutableWorld.toSerializedWorld() = SerializedWorld(
    groundColor = this.groundColor,
    chunks = this.chunks.mapValues { (_, c) -> c.toChunkData() }
)

fun MutableWorld.getChunk(chunk: ChunkRef): MutableChunkData
    = this.chunks.getOrPut(chunk) { MutableChunkData() }


class MutableChunkData(
    val instances: MutableList<ObjectInstance> = mutableListOf()
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
    val serWorld = SerializedWorld.SERIALIZER
        .decodeFromString<SerializedWorld>(jsonWorld)
    return serWorld.toMutableWorld()
}

fun MutableWorld.writeToFile(path: Path) {
    val serWorld: SerializedWorld = this.toSerializedWorld()
    val jsonWorld: String = SerializedWorld.SERIALIZER
        .encodeToString(serWorld)
    Files.writeString(path, jsonWorld)
}


data class ObjectInstanceRef(
    val chunk: ChunkRef,
    val instanceIdx: Int
)