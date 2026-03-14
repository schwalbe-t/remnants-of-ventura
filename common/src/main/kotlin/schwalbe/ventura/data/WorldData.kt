
package schwalbe.ventura.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import schwalbe.ventura.net.SerVector3
import schwalbe.ventura.net.toSerVector3
import org.joml.Vector3f

@Serializable
class RendererConfig(
    val groundToSun: SerVector3,
    val baseColorFactor: SerVector3,
    val shadowColorFactor: SerVector3,
    val outlineColorFactor: SerVector3,
    val defaultLit: Boolean
) {
    companion object {
        val default = RendererConfig(
            groundToSun = Vector3f(1.134f, 1f, 0f).normalize().toSerVector3(),
            baseColorFactor = SerVector3(1f, 1f, 1f),
            shadowColorFactor = SerVector3(0.7f, 0.85f, 1.1f),
            outlineColorFactor = SerVector3(0.55f, 0.65f, 0.75f),
            defaultLit = true
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

@Serializable
data class EnemyPeaceArea(
    val minX: Int, val minZ: Int, val untilX: Int, val untilZ: Int
) {
    fun contains(x: Int, z: Int): Boolean =
        this.minX <= x && x < this.untilX &&
                this.minZ <= z && z < this.untilZ
}

@Serializable
enum class WorldInstanceMode {
    CONSTANT,
    TEMPORARY
}

@Serializable
data class SerializedWorld(
    val info: ConstWorldInfo,
    val groundColor: String,
    val peaceAreas: List<EnemyPeaceArea> = listOf(),
    val chunks: Map<ChunkRef, ChunkData> = mapOf(),
    val instanceMode: WorldInstanceMode = WorldInstanceMode.CONSTANT
) {
    companion object {
        val SERIALIZER = Json {
            allowStructuredMapKeys = true
            prettyPrint = true
            prettyPrintIndent = "\t"
        }
    }
}
