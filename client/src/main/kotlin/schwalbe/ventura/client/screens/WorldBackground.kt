
package schwalbe.ventura.client.screens

import schwalbe.ventura.client.Client
import schwalbe.ventura.client.Renderer
import schwalbe.ventura.client.game.ChunkLoader
import schwalbe.ventura.client.game.ObjectStateProvider
import schwalbe.ventura.data.ChunkRef
import schwalbe.ventura.data.ObjectInstance
import schwalbe.ventura.data.SerializedWorld
import schwalbe.ventura.net.SharedChunkData
import schwalbe.ventura.utils.GroundColorReader
import schwalbe.ventura.client.Camera
import schwalbe.ventura.client.Soundtrack
import schwalbe.ventura.client.game.WorldState
import schwalbe.ventura.data.ObjectProp
import java.nio.file.Files
import java.nio.file.Path

class WorldBackground(
    val displayed: World, val client: Client
) {

    class World(
        worldFile: String, val camera: Camera,
        val triggered: Collection<String> = listOf()
    ) {
        val world: SerializedWorld = SerializedWorld.SERIALIZER
            .decodeFromString(Files.readString(Path.of(worldFile)))
        val groundColor = GroundColorReader(
            Path.of(worldFile).parent.resolve(world.groundColor).toFile()
        )
    }

    val renderer = Renderer(this.client.out3d, camera = this.displayed.camera)
    val chunkLoader = ChunkLoader(this::requestChunks)
    val objectState = object : ObjectStateProvider {
        override fun isTriggered(obj: ObjectInstance)
            = obj[ObjectProp.Triggerable] in displayed.triggered
        override fun worldState(): WorldState.Interpolated? = null
    }

    init {
        val tracklist = Soundtrack[this.displayed.world.info.trackList]
        this.client.soundtrack.changeTracklist(tracklist)
        this.renderer.config = this.displayed.world.info.rendererConfig
    }

    private fun requestChunks(requested: List<ChunkRef>): List<ChunkRef> {
        val groundColor = this.displayed.groundColor
        this.chunkLoader.onChunksReceived(requested.map {
            val data = this.displayed.world.chunks[it]
            it to SharedChunkData(
                instances = data?.instances ?: listOf(),
                groundColorTL = groundColor[it.chunkX,      it.chunkZ],
                groundColorTR = groundColor[it.chunkX + 1,  it.chunkZ],
                groundColorBL = groundColor[it.chunkX,      it.chunkZ + 1],
                groundColorBR = groundColor[it.chunkX + 1,  it.chunkZ + 1],
            )
        })
        return requested
    }

    fun render() {
        this.chunkLoader.update(this.displayed.camera.lookAt, this.objectState)
        this.renderer.update(this.displayed.camera.lookAt)
        this.renderer.forEachPass { pass ->
            this.chunkLoader.render(pass, this.objectState)
        }
    }

    fun dispose() {
        this.renderer.dispose()
        this.chunkLoader.dispose()
    }

}
