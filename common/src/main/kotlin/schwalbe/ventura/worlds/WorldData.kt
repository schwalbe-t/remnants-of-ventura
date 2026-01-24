
package schwalbe.ventura.worlds

import kotlinx.serialization.Serializable
import org.joml.Vector3fc

@Serializable
class RendererConfig(
    val groundToSun: Vector3fc,
    val baseColorFactor: Vector3fc,
    val shadowColorFactor: Vector3fc
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