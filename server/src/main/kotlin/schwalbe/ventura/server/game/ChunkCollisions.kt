
package schwalbe.ventura.server.game

import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3f
import schwalbe.ventura.data.*

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

private fun ObjectTileCollider.applyTransform(
    transf: Matrix4fc
): ObjectTileCollider {
    val posTl = transf.transformPosition(Vector3f(this.left, 0f, this.top))
    val posTr = transf.transformPosition(Vector3f(this.right, 0f, this.top))
    val posBl = transf.transformPosition(Vector3f(this.left, 0f, this.bottom))
    val posBr = transf.transformPosition(Vector3f(this.right, 0f, this.bottom))
    val minX: Float = minOf(posTl.x, posTr.x, posBl.x, posBr.x)
    val maxX: Float = maxOf(posTl.x, posTr.x, posBl.x, posBr.x)
    val minZ: Float = minOf(posTl.z, posTr.z, posBl.z, posBr.z)
    val maxZ: Float = maxOf(posTl.z, posTr.z, posBl.z, posBr.z)
    return ObjectTileCollider(minX, maxX, minZ, maxZ)
}

private fun ObjectTileCollider.contains(tileX: Int, tileZ: Int): Boolean =
    this.left.unitsToUnitIdx() <= tileX &&
    tileX <= this.right.unitsToUnitIdx() &&
    this.top.unitsToUnitIdx() <= tileZ &&
    tileZ <= this.bottom.unitsToUnitIdx()

private inline fun forEachCollidingTile(
    obj: ObjectInstance, crossinline f: (Int, Int) -> Unit
) {
    val relColl = obj[ObjectProp.Type].tileColliderSize ?: return
    val coll = relColl.applyTransform(obj.buildTransform())
    for (tx in coll.left.unitsToUnitIdx()..coll.right.unitsToUnitIdx()) {
        for (tz in coll.top.unitsToUnitIdx()..coll.bottom.unitsToUnitIdx()) {
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
    val staticChunks: Map<ChunkRef, ChunkData>
) {
    typealias DynColliderGenerator = (
        ObjectInstance, World, MutableList<ObjectTileCollider>
    ) -> Unit

    companion object {
        val DYNAMIC_COLLIDERS: Map<ObjectType, DynColliderGenerator> = mapOf(
            ObjectType.AND_GATE to ::andGateDynColliders
        )
    }

    private val byChunk: Map<ChunkRef, BooleanArray>
        = collectChunkColliders(this.staticChunks)
    private val dynamic: MutableList<ObjectTileCollider>
        = mutableListOf()

    fun updateDynamic(world: World) {
        this.dynamic.clear()
        for (chunk in this.staticChunks.values) {
            for (obj in chunk.instances) {
                DYNAMIC_COLLIDERS[obj[ObjectProp.Type]]
                    ?.invoke(obj, world, this.dynamic)
            }
        }
    }

    private fun collidesWithStatic(tileX: Int, tileZ: Int): Boolean {
        val chunkRef = ChunkRef(tileX.unitsToChunks(), tileZ.unitsToChunks())
        val chunk: BooleanArray = this.byChunk[chunkRef] ?: return false
        val chunkTiles: Int = 1.chunksToUnits()
        val relTileX: Int = tileX - (chunkRef.chunkX * chunkTiles)
        val relTileZ: Int = tileZ - (chunkRef.chunkZ * chunkTiles)
        return chunk[chunkFlatIdx(relTileX, relTileZ)]
    }

    private fun collidesWithDynamic(tileX: Int, tileZ: Int): Boolean
        = this.dynamic.any { it.contains(tileX, tileZ) }

    operator fun get(tileX: Int, tileZ: Int): Boolean =
        this.collidesWithStatic(tileX, tileZ) ||
        this.collidesWithDynamic(tileX, tileZ)
}

private fun andGateDynColliders(
    obj: ObjectInstance, world: World, out: MutableList<ObjectTileCollider>
) {
    if (world.triggerables.isTriggered(obj[ObjectProp.Triggerable])) { return }
    out.add(ObjectTileCollider(+0.25f, +0.75f, +0.25f, +0.75f)
        .applyTransform(obj.buildTransform())
    )
}
