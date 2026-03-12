
package schwalbe.ventura.client.game

import schwalbe.ventura.MAX_NUM_REQUESTED_CHUNKS
import schwalbe.ventura.net.*
import schwalbe.ventura.data.*
import schwalbe.ventura.client.*
import schwalbe.ventura.engine.*
import schwalbe.ventura.engine.gfx.*
import kotlin.collections.asSequence
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3fc

private fun computeChunkInstances(
    data: ChunkData
): List<ChunkLoader.LoadedInstance>
    = data.instances.map { inst ->
        val position = inst[ObjectProp.Position]
        val rotation = inst[ObjectProp.Rotation]
        val transform = Matrix4f()
            .translate(position.x, position.y, position.z)
            .rotateXYZ(rotation.x, rotation.y, rotation.z)
            .scale(inst[ObjectProp.Scale])
        ChunkLoader.LoadedInstance(inst, transform)
    }

private fun computeChunkColliders(
    instances: List<ChunkLoader.LoadedInstance>
): List<AxisAlignedBox> {
    val colliders = mutableListOf<AxisAlignedBox>()
    for (instance in instances) {
        val instType: ObjectType = instance.obj[ObjectProp.Type]
        if (!instType.applyColliders) { continue }
        val model: Model<StaticAnim> = ChunkLoader.objectModels
            .getOrNull(instType.ordinal)?.invoke()
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

class ChunkLoader(
    val requestChunks: (List<ChunkRef>) -> List<ChunkRef>,
    val loadRadius: Int = DEFAULT_LOAD_RADIUS,
    val renderRadius: Int = DEFAULT_RENDER_RADIUS
) {

    class LoadedInstance(
        val obj: ObjectInstance,
        val transform: Matrix4fc
    )

    class LoadedChunkData(
        val instances: List<LoadedInstance>,
        val colliders: List<AxisAlignedBox>
    )

    companion object {
        val objectModels: List<Resource<Model<StaticAnim>>> = ObjectType.entries
            .map { Model.loadFile(
                it.modelPath,
                Renderer.meshProperties,
                textureFilter = Texture.Filter.LINEAR
            ) }

        const val DEFAULT_LOAD_RADIUS: Int = 5
        const val DEFAULT_RENDER_RADIUS: Int = 2

        const val OUTLINE_THICKNESS: Float = 0.015f

        fun submitResources(loader: ResourceLoader) {
            this.objectModels.forEach(loader::submit)
        }
    }


    var centerX: Int = 0
        private set
    var centerZ: Int = 0
        private set
    val requested: MutableSet<ChunkRef> = mutableSetOf()
    val loaded: MutableMap<ChunkRef, LoadedChunkData> = mutableMapOf()

    fun onChunksReceived(chunks: List<Pair<ChunkRef, ChunkData>>) {
        for ((ref, data) in chunks) {
            val instances = computeChunkInstances(data)
            val colliders = computeChunkColliders(instances)
            this.loaded[ref] = LoadedChunkData(instances, colliders)
        }
    }

    fun invalidateChunk(chunk: ChunkRef) {
        this.requested.remove(chunk)
        this.loaded.remove(chunk)
    }

    private fun chunksInRange(r: Int): Sequence<ChunkRef>
        = ((-r)..(+r)).asSequence().flatMap { x ->
            ((-r)..(+r)).asSequence().map { z ->
                ChunkRef(this.centerX + x, this.centerZ + z)
            }
        }

    private fun collectMissingChunks(): List<ChunkRef>
        = this.chunksInRange(this.loadRadius)
        .filter { it !in this.requested }
        .toList()

    private fun requestChunkData(chunks: List<ChunkRef>) {
        this.requested.addAll(this.requestChunks(chunks))
    }

    fun update(center: Vector3fc) {
        this.centerX = center.x().unitsToChunkIdx()
        this.centerZ = center.z().unitsToChunkIdx()
        val missing: List<ChunkRef> = this.collectMissingChunks()
        this.requestChunkData(missing)
    }

    fun intersectsAnyLoaded(b: AxisAlignedBox): Boolean {
        val mcsc: Int = MAX_COLLIDER_SIZE_CHUNKS
        val minChunkX: Int = b.min.x().unitsToChunkIdx() - mcsc
        val maxChunkX: Int = b.max.x().unitsToChunkIdx() + mcsc
        val minChunkZ: Int = b.min.z().unitsToChunkIdx() - mcsc
        val maxChunkZ: Int = b.max.z().unitsToChunkIdx() + mcsc
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

    private fun renderChunk(data: LoadedChunkData, pass: RenderPass) {
        val groupedInstances: MutableMap<ObjectType, MutableList<Matrix4fc>>
            = mutableMapOf()
        for (inst in data.instances) {
            val instType: ObjectType = inst.obj[ObjectProp.Type]
            val group: MutableList<Matrix4fc> = groupedInstances
                .getOrPut(instType) { mutableListOf() }
            group.add(inst.transform)
        }
        for ((type, instances) in groupedInstances) {
            val model: Model<StaticAnim> = objectModels
                .getOrNull(type.ordinal)?.invoke()
                ?: continue
            if (type.renderOutline) {
                pass.renderOutline(
                    model, OUTLINE_THICKNESS, animState = null, instances
                )
            }
            pass.renderGeometry(model, animState = null, instances)
        }
    }

    fun render(pass: RenderPass) {
        for (chunk in this.chunksInRange(this.renderRadius)) {
            val data: LoadedChunkData = this.loaded[chunk] ?: continue
            this.renderChunk(data, pass)
        }
    }

}

fun ChunkLoader.Companion.requestChunksFromNetwork(
    network: NetworkClient
): (List<ChunkRef>) -> List<ChunkRef> = request@{ chunks ->
    val outPackets = network.outPackets ?: return@request listOf()
    var numRequested = 0
    while (numRequested < chunks.size) {
        val numRemaining: Int = chunks.size - numRequested
        val batchSize: Int = minOf(numRemaining, MAX_NUM_REQUESTED_CHUNKS)
        val batch: List<ChunkRef> = chunks
            .subList(numRequested, numRequested + batchSize)
        outPackets.send(Packet.serialize(
            PacketType.REQUEST_CHUNK_CONTENTS,
            RequestedChunksPacket(batch)
        ))
        numRequested += batchSize
    }
    chunks
}