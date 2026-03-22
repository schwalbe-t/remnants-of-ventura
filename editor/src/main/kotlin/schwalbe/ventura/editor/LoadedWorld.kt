
package schwalbe.ventura.editor

import schwalbe.ventura.client.game.ChunkLoader
import schwalbe.ventura.data.*
import schwalbe.ventura.utils.SerVector3
import schwalbe.ventura.net.SharedChunkData
import schwalbe.ventura.utils.GroundColorReader
import java.nio.file.Path

class LoadedWorld(val path: Path) {

    val world = MutableWorld.readFromFile(this.path)
    val groundColor = GroundColorReader(
        textureFile = this.path.parent.resolve(this.world.groundColor).toFile()
    )
    var lastModified: Long? = null
    val chunkLoader = ChunkLoader(
        requestChunks = this::loadChunks,
        loadRadius = 5,
        renderRadius = 5
    )
    var selectedObject: ObjectInstanceRef? = null

    private fun loadChunks(requested: List<ChunkRef>): List<ChunkRef> {
        this.chunkLoader.onChunksReceived(requested.map { c ->
            this.world.chunks[c].let { c to SharedChunkData(
                instances = it?.instances ?: listOf(),
                groundColorTL = this.groundColor[c.chunkX,      c.chunkZ    ],
                groundColorTR = this.groundColor[c.chunkX + 1,  c.chunkZ    ],
                groundColorBL = this.groundColor[c.chunkX,      c.chunkZ + 1],
                groundColorBR = this.groundColor[c.chunkX + 1,  c.chunkZ + 1]
            ) }
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
            newObjPos.x.unitsToChunkIdx(), newObjPos.z.unitsToChunkIdx()
        )
        val newObjChunk = this.world.getChunk(newObjChunkRef)
        val newObjInstIdx: Int = newObjChunk.instances.size
        newObjChunk.instances.add(newObj)
        this.onChunkEdited(newObjChunkRef)
        val newObjRef = ObjectInstanceRef(newObjChunkRef, newObjInstIdx)
        return newObjRef
    }

}