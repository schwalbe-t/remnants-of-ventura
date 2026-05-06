
package schwalbe.ventura.client.game

import schwalbe.ventura.engine.*
import schwalbe.ventura.client.*
import schwalbe.ventura.data.ObjectInstance
import schwalbe.ventura.data.ObjectProp
import schwalbe.ventura.net.WorldInfoPacket
import schwalbe.ventura.net.WorldStatePacket
import kotlin.uuid.Uuid
import org.joml.*

class World(client: Client, info: WorldInfoPacket) {

    companion object {
        fun submitResources(resLoader: ResourceLoader) {
            Person.submitResources(resLoader)
            Robot.submitResources(resLoader)
            ChunkLoader.submitResources(resLoader)
            VisualEffects.submitResources(resLoader)
        }
    }

    class WorldObjectStateProvider(
        val state: WorldState
    ) : ObjectStateProvider {
        override fun isTriggered(obj: ObjectInstance): Boolean
            = obj[ObjectProp.Triggerable] in state.interpolated.triggeredObjects
        override fun lastWorldState(): WorldStatePacket?
            = this.state.lastReceived
    }


    val id: Uuid = info.worldId
    val isMain: Boolean = info.isMainWorld

    val player = Player(info.position, info.playerStyle)
    val chunks = ChunkLoader(
        ChunkLoader.requestChunksFromNetwork(client.network)
    )
    val state = WorldState()
    val vfx = VisualEffects()
    val objectStateProvider = WorldObjectStateProvider(this.state)

    val playerAtCenterCamMode = CameraController.Mode(
        lookAt = { _ -> Vector3f()
            .add(this.player.position)
            .add(0f, +1f, 0f)
        },
        fovDegrees = 30f
    )
    val camController = CameraController(this.playerAtCenterCamMode)

    init {
        client.renderer.config = info.worldInfo.rendererConfig
        client.soundtrack.changeTracklist(Soundtrack[info.worldInfo.trackList])
    }

    fun dispose() {
        this.chunks.dispose()
    }

}

fun World.update(client: Client, captureInput: Boolean) {
    this.player.update(client, captureInput)
    this.camController.update(
        client.renderer.camera, client.renderer, captureInput
    )
    this.chunks.update(this.player.position, this.objectStateProvider)
    this.state.update(client)
}

fun World.render(client: Client) {
    client.renderer.sunDiameter = 5f + this.camController.distance.value * 1.25f
    val sunTarget = Vector3f(0f, 0f, -this.camController.distance.value * 0.25f)
        .add(this.player.position)
    client.renderer.update(sunTarget)
    client.renderer.forEachPass { pass ->
        this.chunks.render(pass, this.objectStateProvider)
        this.state.render(client, pass)
        this.player.render(pass)
        this.vfx.render(pass, client.deltaTime, this.state)
    }
}