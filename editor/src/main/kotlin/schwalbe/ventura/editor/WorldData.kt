
package schwalbe.ventura.editor

import schwalbe.ventura.data.*
import schwalbe.ventura.net.SerVector3
import java.nio.file.Files
import java.nio.file.Path


class MutableWorld(
    val info: ConstWorldInfo,
    val groundColor: String,
    val peaceAreas: List<EnemyPeaceArea>,
    val chunks: MutableMap<ChunkRef, MutableChunkData> = mutableMapOf(),
    val instanceMode: WorldInstanceMode,
    val startPosition: SerVector3
) {
    companion object
}

fun SerializedWorld.toMutableWorld() = MutableWorld(
    info = this.info,
    groundColor = this.groundColor,
    peaceAreas = this.peaceAreas,
    chunks = this.chunks
        .mapValuesTo(mutableMapOf()) { (_, c) -> c.toMutableChunkData() },
    instanceMode = this.instanceMode,
    startPosition = this.startPosition
)

fun MutableWorld.toSerializedWorld() = SerializedWorld(
    info = this.info,
    groundColor = this.groundColor,
    peaceAreas = this.peaceAreas,
    chunks = this.chunks.mapValues { (_, c) -> c.toChunkData() },
    instanceMode = this.instanceMode,
    startPosition = this.startPosition
)

fun MutableWorld.getChunk(chunk: ChunkRef): MutableChunkData
    = this.chunks.getOrPut(chunk) { MutableChunkData() }

fun MutableWorld.clean(): MutableWorld {
    this.chunks.values.removeIf { it.instances.isEmpty() }
    return this
}


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