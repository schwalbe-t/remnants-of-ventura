
package schwalbe.ventura.worlds

import kotlinx.serialization.Serializable
import schwalbe.ventura.net.SerVector3

@Serializable
class RendererConfig(
    val groundToSun: SerVector3,
    val baseColorFactor: SerVector3,
    val shadowColorFactor: SerVector3
)

@Serializable
data class ConstWorldInfo(
    val rendererConfig: RendererConfig
)

@Serializable
data class ChunkRef(val chunkX: Int, val chunkZ: Int)

@Serializable
data class ChunkData(
    val instances: List<ObjectInstance>
)

class WorldData(
    val info: ConstWorldInfo,
    val chunks: Map<ChunkRef, ChunkData>
)