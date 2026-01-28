
package schwalbe.ventura.worlds

import kotlinx.serialization.Serializable
import schwalbe.ventura.net.SerVector3
import schwalbe.ventura.net.toSerVector3
import org.joml.Vector3f

@Serializable
class RendererConfig(
    val groundToSun: SerVector3,
    val baseColorFactor: SerVector3,
    val shadowColorFactor: SerVector3,
    val outlineColorFactor: SerVector3
) {
    companion object {
        val default = RendererConfig(
            groundToSun = Vector3f(1.134f, 1f, 0f).normalize().toSerVector3(),
            baseColorFactor = SerVector3(1f, 1f, 1f),
            shadowColorFactor = SerVector3(0.7f, 0.85f, 1.1f),
            outlineColorFactor = SerVector3(0.55f, 0.65f, 0.75f)
        )
    }
}

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