
package schwalbe.ventura.server.game

import org.joml.Matrix4f
import org.joml.Vector3f
import schwalbe.ventura.data.*
import schwalbe.ventura.utils.IntPair
import schwalbe.ventura.utils.SerVector3
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private fun findChunkBounds(
    chunks: Collection<ChunkRef>
): Pair<IntRange, IntRange>? {
    if (chunks.isEmpty()) { return null }
    val minChunkX: Int = chunks.minOf(ChunkRef::chunkX)
    val maxChunkX: Int = chunks.maxOf(ChunkRef::chunkX)
    val minChunkZ: Int = chunks.minOf(ChunkRef::chunkZ)
    val maxChunkZ: Int = chunks.maxOf(ChunkRef::chunkZ)
    return minChunkX..maxChunkX to minChunkZ..maxChunkZ
}

private fun chunkFlatIdx(relTx: Int, relTz: Int): Int
    = relTz * 1.chunksToUnits() + relTx

private inline fun forEachCollidingTile(
    obj: ObjectInstance, crossinline f: (Int, Int) -> Unit
) {
    val coll = obj[ObjectProp.Type].tileColliderSize ?: return
    val transf: Matrix4f = obj.buildTransform()
    val posTl = transf.transformPosition(Vector3f(coll.left, 0f, coll.top))
    val posTr = transf.transformPosition(Vector3f(coll.right, 0f, coll.top))
    val posBl = transf.transformPosition(Vector3f(coll.left, 0f, coll.bottom))
    val posBr = transf.transformPosition(Vector3f(coll.right, 0f, coll.bottom))
    val minX: Float = minOf(posTl.x, posTr.x, posBl.x, posBr.x)
    val maxX: Float = maxOf(posTl.x, posTr.x, posBl.x, posBr.x)
    val minZ: Float = minOf(posTl.z, posTr.z, posBl.z, posBr.z)
    val maxZ: Float = maxOf(posTl.z, posTr.z, posBl.z, posBr.z)
    for (tx in minX.unitsToUnitIdx()..maxX.unitsToUnitIdx()) {
        for (tz in minZ.unitsToUnitIdx()..maxZ.unitsToUnitIdx()) {
            f(tx, tz)
        }
    }
}

private fun collectChunkColliders(
    chunks: Map<ChunkRef, ChunkData>
): Map<ChunkRef, BooleanArray> {
    val (rangeX, rangeZ) = findChunkBounds(chunks.keys) ?: return mapOf()
    val colliders: MutableMap<ChunkRef, BooleanArray> = mutableMapOf()
    val chunkTiles: Int = 1.chunksToUnits()
    val mcsc: Int = MAX_COLLIDER_SIZE_CHUNKS
    for (chunkX in (rangeX.first - mcsc)..(rangeX.last + mcsc)) {
        for (chunkZ in (rangeZ.first - mcsc)..(rangeZ.last + mcsc)) {
            val byTiles = BooleanArray(chunkTiles * chunkTiles) { false }
            colliders[ChunkRef(chunkX, chunkZ)] = byTiles
        }
    }
    for (chunkData in chunks.values) {
        for (obj in chunkData.instances) {
            forEachCollidingTile(obj) f@{ absTx, absTz ->
                val chunkX: Int = absTx.unitsToChunks()
                val chunkZ: Int = absTz.unitsToChunks()
                val chunkR = ChunkRef(chunkX, chunkZ)
                val chunk: BooleanArray = colliders[chunkR] ?: return@f
                val relTx: Int = absTx - chunkX.chunksToUnits()
                val relTz: Int = absTz - chunkZ.chunksToUnits()
                chunk[chunkFlatIdx(relTx, relTz)] = true
            }
        }
    }
    return colliders
}

class ChunkCollisions(
    chunks: Map<ChunkRef, ChunkData>
) {
    private val byChunk: Map<ChunkRef, BooleanArray>
        = collectChunkColliders(chunks)

    operator fun get(tileX: Int, tileZ: Int): Boolean {
        val chunkRef = ChunkRef(tileX.unitsToChunks(), tileZ.unitsToChunks())
        val chunk = this.byChunk[chunkRef] ?: return false
        val chunkTiles: Int = 1.chunksToUnits()
        val relTileX: Int = tileX - (chunkRef.chunkX * chunkTiles)
        val relTileZ: Int = tileZ - (chunkRef.chunkZ * chunkTiles)
        return chunk[chunkFlatIdx(relTileX, relTileZ)]
    }
}
