
package schwalbe.ventura.server.game

import schwalbe.ventura.data.ChunkData
import schwalbe.ventura.data.ChunkRef
import schwalbe.ventura.data.ConstWorldInfo
import schwalbe.ventura.data.MAX_COLLIDER_SIZE_CHUNKS
import schwalbe.ventura.data.chunksToUnits
import schwalbe.ventura.data.unitsToChunkIdx
import schwalbe.ventura.data.unitsToChunks
import schwalbe.ventura.data.unitsToUnitIdx

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
            val objCTx: Int = obj.position.x.unitsToUnitIdx()
            val objCTz: Int = obj.position.z.unitsToUnitIdx()
            val r: Int = obj.type.tileColliderRadius - 1
            if (r < 0) { continue }
            for (objTx in (objCTx - r)..(objCTx + r)) {
                for (objTz in (objCTz - r)..(objCTz + r)) {
                    val objChx: Int = objTx.unitsToChunks()
                    val objChz: Int = objTz.unitsToChunks()
                    val objCh = ChunkRef(objChx, objChz)
                    val chunkColl: BooleanArray = colliders[objCh] ?: continue
                    val objRTx: Int = objTx - (objChx * chunkTiles)
                    val objRTz: Int = objTz - (objChz * chunkTiles)
                    chunkColl[chunkFlatIdx(objRTx, objRTz)] = true
                }
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

class WorldData(
    val info: ConstWorldInfo,
    val chunks: Map<ChunkRef, ChunkData>
) {
    val chunkCollisions: ChunkCollisions = ChunkCollisions(this.chunks)
}