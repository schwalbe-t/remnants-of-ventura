
package schwalbe.ventura.client.game

import schwalbe.ventura.MAX_NUM_REQUESTED_CHUNKS
import schwalbe.ventura.net.*
import schwalbe.ventura.net.PacketType.*
import schwalbe.ventura.data.*
import schwalbe.ventura.client.Client
import schwalbe.ventura.engine.*
import schwalbe.ventura.engine.gfx.*
import kotlin.collections.asSequence
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3fc

private val objectModels: List<Resource<Model<StaticAnim>>> = ObjectType.entries
    .map {
        Model.loadFile(
            it.modelPath,
            Renderer.meshProperties,
            textureFilter = Texture.Filter.LINEAR
        )
    }

private fun computeChunkInstances(
    data: ChunkData
): List<ChunkLoader.LoadedInstance>
    = data.instances.map { inst ->
        val transform = Matrix4f()
            .translate(inst.position.x, inst.position.y, inst.position.z)
            .rotateXYZ(inst.rotation.x, inst.rotation.y, inst.rotation.z)
            .scale(inst.scale.x, inst.scale.y, inst.scale.z)
        ChunkLoader.LoadedInstance(inst, transform)
    }

private fun computeChunkColliders(
    instances: List<ChunkLoader.LoadedInstance>
): List<AxisAlignedBox> {
    val colliders = mutableListOf<AxisAlignedBox>()
    for (instance in instances) {
        if (!instance.obj.type.applyColliders) { continue }
        val model: Model<StaticAnim> = objectModels
            .getOrNull(instance.obj.type.ordinal)?.invoke()
            ?: continue
        model.forEachNode { node ->
            for (meshI in node.meshes) {
                val mesh = model.meshes.getOrNull(meshI) ?: continue
                val collider = mesh.bounds.toMutableAxisBox()
                    .transform(node.localTransform)
                    .transform(instance.transform)
                colliders.add(collider)
            }
        }
    }
    return colliders
}

class ChunkLoader {

    class LoadedInstance(
        val obj: ObjectInstance,
        val transform: Matrix4fc
    )

    class LoadedChunkData(
        val instances: List<LoadedInstance>,
        val colliders: List<AxisAlignedBox>
    )

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
    val loaded: MutableMap<ChunkRef, LoadedChunkData> = mutableMapOf()

    fun handleReceivedChunks(contents: ChunkContentsPacket) {
        for ((ref, data) in contents.chunks) {
            val instances = computeChunkInstances(data)
            val colliders = computeChunkColliders(instances)
            this.loaded[ref] = LoadedChunkData(instances, colliders)
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

    fun intersectsAnyLoaded(b: AxisAlignedBox): Boolean {
        val minChunkX: Int = b.min.x().unitsToChunkIdx() - 1
        val minChunkZ: Int = b.min.z().unitsToChunkIdx() - 1
        val maxChunkX: Int = b.max.x().unitsToChunkIdx() + 1
        val maxChunkZ: Int = b.max.z().unitsToChunkIdx() + 1
        for (chunkX in minChunkX..maxChunkX) {
            for (chunkZ in minChunkZ..maxChunkZ) {
                val chunk = this.loaded[ChunkRef(chunkX, chunkZ)] ?: continue
                if (chunk.colliders.any { it.intersects(b) }) {
                    return true
                }
            }
        }
        return false
    }

    private fun renderChunk(data: LoadedChunkData, client: Client) {
        val groupedInstances: MutableMap<ObjectType, MutableList<Matrix4fc>>
            = mutableMapOf()
        for (inst in data.instances) {
            val group: MutableList<Matrix4fc> = groupedInstances
                .getOrPut(inst.obj.type) { mutableListOf() }
            group.add(inst.transform)
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
            val data: LoadedChunkData = this.loaded[chunk] ?: continue
            this.renderChunk(data, client)
        }
    }

}
