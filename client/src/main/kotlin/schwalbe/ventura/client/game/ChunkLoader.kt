
package schwalbe.ventura.client.game

import schwalbe.ventura.MAX_NUM_REQUESTED_CHUNKS
import schwalbe.ventura.net.*
import schwalbe.ventura.data.*
import schwalbe.ventura.client.*
import schwalbe.ventura.engine.*
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.utils.SerVector3
import kotlin.collections.asSequence
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3fc
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.math.abs

object GroundVert : VertShaderDef<GroundVert> {
    override val path: String = "shaders/ground.vert.glsl"

    val renderer = RendererVert<GroundVert>()
}

object GroundFrag : FragShaderDef<GroundFrag> {
    override val path: String = "shaders/ground.frag.glsl"

    val renderer = RendererFrag<GroundFrag>()
}

val groundShader: Resource<Shader<GroundVert, GroundFrag>>
    = Shader.loadGlsl(GroundVert, GroundFrag)

private fun computeChunkInstances(
    data: SharedChunkData
): List<ChunkLoader.LoadedInstance>
    = data.instances.map { inst ->
        val transform = Objects.baseTransform(inst)
        val colliders = computeInstanceColliders(inst, transform)
        ChunkLoader.LoadedInstance(inst, transform, colliders)
    }

private fun computeInstanceColliders(
    obj: ObjectInstance, transform: Matrix4fc
): List<AxisAlignedBox> {
    val colliders = mutableListOf<AxisAlignedBox>()
    val instType: ObjectType = obj[ObjectProp.Type]
    val model: Model<StaticAnim> = ChunkLoader.objectModels
        .getOrNull(instType.ordinal)?.invoke()
        ?: return listOf()
    model.forEachNode { node ->
        for (meshI in node.meshes) {
            val mesh = model.meshes.getOrNull(meshI) ?: continue
            val collider = mesh.bounds.toMutableAxisBox()
                .transform(node.localTransform)
                .transform(transform)
            colliders.add(collider)
        }
    }
    return colliders
}

fun buildChunkGroundGeometry(ref: ChunkRef, data: SharedChunkData): Geometry {
    val vertSize: Int = ChunkLoader.GROUND_GEOMETRY_ATTRIBS
        .sumOf { it.numBytes }
    val vbo = ByteBuffer
        .allocateDirect(vertSize * 4)
        .order(ByteOrder.nativeOrder())
    fun ByteBuffer.putVertex(chX: Int, chZ: Int, color: SerVector3) {
        // vec3 position
        putFloat(chX.chunksToUnits().toFloat())
        putFloat(0f)
        putFloat(chZ.chunksToUnits().toFloat())
        // vec3 color
        putFloat(color.x)
        putFloat(color.y)
        putFloat(color.z)
    }
    vbo.putVertex(ref.chunkX,       ref.chunkZ,     data.groundColorTL) // [0]
    vbo.putVertex(ref.chunkX + 1,   ref.chunkZ,     data.groundColorTR) // [1]
    vbo.putVertex(ref.chunkX,       ref.chunkZ + 1, data.groundColorBL) // [2]
    vbo.putVertex(ref.chunkX + 1,   ref.chunkZ + 1, data.groundColorBR) // [3]
    vbo.flip()
    val ebo = ByteBuffer
        .allocateDirect(6 /* length */ * 2 /* sizeof(short) */)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()
    fun ShortBuffer.putElement(a: Short, b: Short, c: Short) {
        put(a)
        put(b)
        put(c)
    }
    val cutDir = ((ref.chunkX * 73856093) xor (ref.chunkZ * 19349663)) and 1
    if (cutDir == 0) {
        ebo.putElement(2, 1, 0) // bl -> tr -> tl
        ebo.putElement(1, 2, 3) // tr -> bl -> br
    } else {
        ebo.putElement(2, 3, 0) // bl -> br -> tl
        ebo.putElement(1, 0, 3) // tr -> tl -> br
    }
    ebo.flip()
    return Geometry(ChunkLoader.GROUND_GEOMETRY_ATTRIBS, vbo, ebo)
}

class ChunkLoader(
    val requestChunks: (List<ChunkRef>) -> List<ChunkRef>,
    val loadRadius: Int = DEFAULT_LOAD_RADIUS,
    val renderRadius: Int = DEFAULT_RENDER_RADIUS
) {

    class LoadedInstance(
        val obj: ObjectInstance,
        var transform: Matrix4fc,
        var colliders: List<AxisAlignedBox>
    ) {
        fun update(state: ObjectStateProvider) {
            Objects.update(state, this.obj)
            val newTransf: Matrix4f = Objects.transform(state, this.obj)
                ?: return
            this.transform = newTransf
            this.colliders = computeInstanceColliders(this.obj, newTransf)
        }
    }

    class LoadedChunkData(
        val instances: List<LoadedInstance>,
        val ground: Geometry
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

        val GROUND_GEOMETRY_ATTRIBS: List<Geometry.Attribute> = listOf(
            Geometry.float(3),
            Geometry.float(3)
        )
        val GROUND_INSTANCES: List<Matrix4fc> = listOf(Matrix4f())

        fun submitResources(loader: ResourceLoader) {
            this.objectModels.forEach(loader::submit)
            loader.submit(groundShader)
        }
    }


    var centerX: Int = 0
        private set
    var centerZ: Int = 0
        private set
    val requested: MutableSet<ChunkRef> = mutableSetOf()
    val loaded: MutableMap<ChunkRef, LoadedChunkData> = mutableMapOf()

    fun onChunksReceived(chunks: List<Pair<ChunkRef, SharedChunkData>>) {
        for ((ref, data) in chunks) {
            val instances = computeChunkInstances(data)
            val ground = buildChunkGroundGeometry(ref, data)
            this.loaded[ref] = LoadedChunkData(instances, ground)
        }
    }

    private fun removeLoaded(ref: ChunkRef) {
        val removed = this.loaded.remove(ref) ?: return
        removed.ground.dispose()
    }

    fun invalidateChunk(chunk: ChunkRef) {
        this.requested.remove(chunk)
        this.removeLoaded(chunk)
    }

    private fun unloadChunks() {
        for (ref in this.loaded.keys.toList()) {
            val dx: Int = abs(ref.chunkX - this.centerX)
            val dz: Int = abs(ref.chunkZ - this.centerZ)
            if (maxOf(dx, dz) <= this.loadRadius) { continue }
            this.invalidateChunk(ref)
        }
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

    fun update(center: Vector3fc, state: ObjectStateProvider) {
        this.centerX = center.x().unitsToChunkIdx()
        this.centerZ = center.z().unitsToChunkIdx()
        this.unloadChunks()
        val missing: List<ChunkRef> = this.collectMissingChunks()
        if (missing.isNotEmpty()) {
            this.requestChunkData(missing)
        }
        for (chunk in this.loaded.values) {
            for (inst in chunk.instances) {
                inst.update(state)
            }
        }
    }

    fun findIntersecting(
        b: AxisAlignedBox, includeAll: Boolean = false
    ): ObjectInstance? {
        val mcsc: Int = MAX_COLLIDER_SIZE_CHUNKS
        val minChunkX: Int = b.min.x().unitsToChunkIdx() - mcsc
        val maxChunkX: Int = b.max.x().unitsToChunkIdx() + mcsc
        val minChunkZ: Int = b.min.z().unitsToChunkIdx() - mcsc
        val maxChunkZ: Int = b.max.z().unitsToChunkIdx() + mcsc
        for (chunkX in minChunkX..maxChunkX) {
            for (chunkZ in minChunkZ..maxChunkZ) {
                val chunk = this.loaded[ChunkRef(chunkX, chunkZ)] ?: continue
                for (instance in chunk.instances) {
                    val type = instance.obj[ObjectProp.Type]
                    if (!type.applyColliders && !includeAll) { continue }
                    if (instance.colliders.any { it.intersects(b) }) {
                        return instance.obj
                    }
                }
            }
        }
        return null
    }

    private fun renderChunk(
        state: ObjectStateProvider, data: LoadedChunkData, pass: RenderPass
    ) {
        val groupedInstances: MutableMap<ObjectType, MutableList<Matrix4fc>>
            = mutableMapOf()
        for (inst in data.instances) {
            Objects.render(pass, state, inst.obj, inst.transform) otherwise@{
                val instType: ObjectType = inst.obj[ObjectProp.Type]
                val group: MutableList<Matrix4fc> = groupedInstances
                    .getOrPut(instType) { mutableListOf() }
                group.add(inst.transform)
            }
        }
        for ((type, instances) in groupedInstances) {
            val model: Model<StaticAnim> = objectModels
                .getOrNull(type.ordinal)?.invoke()
                ?: continue
            Objects.renderBatch(pass, type.renderOutline, model, instances)
        }
        pass.render(
            data.ground, groundShader(),
            GroundVert.renderer, GroundFrag.renderer,
            GROUND_INSTANCES
        )
    }

    fun render(pass: RenderPass, state: ObjectStateProvider) {
        for (chunk in this.chunksInRange(this.renderRadius)) {
            val data: LoadedChunkData = this.loaded[chunk] ?: continue
            this.renderChunk(state, data, pass)
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