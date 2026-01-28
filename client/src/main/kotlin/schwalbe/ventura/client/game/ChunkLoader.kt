
package schwalbe.ventura.client.game

import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3fc
import schwalbe.ventura.MAX_NUM_REQUESTED_CHUNKS
import schwalbe.ventura.net.*
import schwalbe.ventura.net.PacketType.*
import schwalbe.ventura.worlds.*
import schwalbe.ventura.client.Client
import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.gfx.*
import kotlin.collections.asSequence

private val objectModels: List<Resource<Model<StaticAnim>>> = ObjectType.entries
    .map {
        Model.loadFile(
            it.filePath,
            Renderer.meshProperties,
            textureFilter = Texture.Filter.LINEAR
        )
    }

class ChunkLoader {

    companion object {
        const val LOAD_RADIUS: Int = 5
        const val RENDER_RADIUS: Int = 2

        const val OUTLINE_THICKNESS: Float = 0.015f

        fun submitResources(loader: ResourceLoader) {
            objectModels.forEach(loader::submit)
        }
    }


    var centerX: Int = 0
        private set
    var centerZ: Int = 0
        private set
    val requested: MutableSet<ChunkRef> = mutableSetOf()
    val loaded: MutableMap<ChunkRef, ChunkData> = mutableMapOf()

    fun handleReceivedChunks(contents: ChunkContentsPacket) {
        for ((ref, data) in contents.chunks) {
            this.loaded[ref] = data
        }
    }

    private fun chunksInRange(r: Int): Sequence<ChunkRef>
        = (-r..+r).asSequence().flatMap { x ->
            (-r..+r).asSequence().map { z ->
                ChunkRef(this.centerX + x, this.centerZ + z)
            }
        }

    private fun collectMissingChunks(): List<ChunkRef>
        = this.chunksInRange(ChunkLoader.LOAD_RADIUS)
        .filter { it !in this.requested }
        .toList()

    private fun requestChunkData(chunks: List<ChunkRef>, client: Client) {
        var numRequested: Int = 0
        while (numRequested < chunks.size) {
            val numRemaining: Int = chunks.size - numRequested
            val batchSize: Int = minOf(numRemaining, MAX_NUM_REQUESTED_CHUNKS)
            val batch: List<ChunkRef> = chunks
                .subList(numRequested, numRequested + batchSize)
            println("Requesting data for $batchSize chunk(s)")
            client.network.outPackets?.send(Packet.serialize(
                UP_REQUEST_CHUNK_CONTENTS, RequestedChunksPacket(batch)
            ))
            batch.forEach(this.requested::add)
            numRequested += batchSize
        }
    }

    fun update(client: Client, center: Vector3fc) {
        this.centerX = center.x().unitsToChunkIdx()
        this.centerZ = center.z().unitsToChunkIdx()
        val missing: List<ChunkRef> = this.collectMissingChunks()
        this.requestChunkData(missing, client)
    }

    private fun renderChunk(data: ChunkData, client: Client) {
        val groupedInstances: MutableMap<ObjectType, MutableList<Matrix4fc>>
            = mutableMapOf()
        for (inst in data.instances) {
            val group: MutableList<Matrix4fc> = groupedInstances
                .getOrPut(inst.type) { mutableListOf() }
            group.add(Matrix4f()
                .translate(inst.position.x, inst.position.y, inst.position.z)
                .rotateXYZ(inst.rotation.x, inst.rotation.y, inst.rotation.z)
                .scale(inst.scale.x, inst.scale.y, inst.scale.z)
            )
        }
        for ((type, instances) in groupedInstances) {
            val model: Model<StaticAnim> = objectModels
                .getOrNull(type.ordinal)?.invoke()
                ?: continue
            if (type.renderOutline) {
                client.renderer.renderOutline(
                    model, ChunkLoader.OUTLINE_THICKNESS,
                    animState = null, instances
                )
            }
            client.renderer.renderGeometry(
                model, animState = null, instances
            )
        }
    }

    fun render(client: Client) {
        for (chunk in this.chunksInRange(ChunkLoader.RENDER_RADIUS)) {
            val data: ChunkData = this.loaded[chunk] ?: continue
            this.renderChunk(data, client)
        }
    }

}
