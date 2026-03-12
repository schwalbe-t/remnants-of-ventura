
package schwalbe.ventura.editor

import schwalbe.ventura.client.game.ChunkLoader
import schwalbe.ventura.data.*
import schwalbe.ventura.net.SerVector3
import java.nio.file.Path

class LoadedWorld(val path: Path) {

    val world = MutableWorld.readFromFile(this.path)
    var lastModified: Long? = null
    val chunkLoader = ChunkLoader(
        requestChunks = this::loadChunks,
        loadRadius = 5,
        renderRadius = 5
    )
    var selectedObject: ObjectInstanceRef? = null

    private fun loadChunks(requested: List<ChunkRef>): List<ChunkRef> {
        this.chunkLoader.onChunksReceived(requested.mapNotNull { c ->
            this.world.chunks[c]?.let { c to it.toChunkData() }
        })
        return requested
    }

    fun onEdited() {
        this.lastModified = System.currentTimeMillis()
    }

    fun onChunkEdited(chunk: ChunkRef) {
        this.onEdited()
        this.chunkLoader.invalidateChunk(chunk)
    }

    fun withObjectEdit(
        oldObjRef: ObjectInstanceRef,
        f: (ObjectInstance) -> ObjectInstance
    ): ObjectInstanceRef {
        val oldObj: ObjectInstance = this.world.getChunk(oldObjRef.chunk)
            .instances.removeAt(oldObjRef.instanceIdx)
        this.onChunkEdited(oldObjRef.chunk)
        val newObj: ObjectInstance = f(oldObj)
        val newObjPos: SerVector3 = newObj[ObjectProp.Position]
        val newObjChunkRef = ChunkRef(
            newObjPos.x.unitsToUnitIdx(), newObjPos.z.unitsToUnitIdx()
        )
        val newObjChunk = this.world.getChunk(newObjChunkRef)
        val newObjInstIdx: Int = newObjChunk.instances.size
        newObjChunk.instances.add(newObj)
        this.onChunkEdited(newObjChunkRef)
        val newObjRef = ObjectInstanceRef(newObjChunkRef, newObjInstIdx)
        return newObjRef
    }

}