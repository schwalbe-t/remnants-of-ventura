
package schwalbe.ventura.data

import schwalbe.ventura.utils.SerVector3
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
            groundToSun = SerVector3(0.2f, 0.25f, 0f),
            baseColorFactor = SerVector3(1.2f, 0.95f, 0.75f),
            shadowColorFactor = SerVector3(0.7f, 0.75f, 0.9f),
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
    val instanceMode: WorldInstanceMode = WorldInstanceMode.CONSTANT,
    val info: ConstWorldInfo,
    val groundColor: String,
    val startPosition: SerVector3 = SerVector3(0f, 0f, 0f),
    val peaceAreas: List<EnemyPeaceArea> = listOf(),
    val chunks: Map<ChunkRef, ChunkData> = mapOf(),
) {
    companion object {
        val SERIALIZER = Json {
            allowStructuredMapKeys = true
            prettyPrint = true
            prettyPrintIndent = "\t"
            encodeDefaults = true
        }
    }
}
