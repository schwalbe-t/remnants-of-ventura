
package schwalbe.ventura.client.game

import schwalbe.ventura.engine.*
import schwalbe.ventura.client.*
import org.joml.*
import kotlin.uuid.Uuid

class World(
    client: Client,
    val id: Uuid,
    val isMain: Boolean
) {

    companion object {
        fun submitResources(resLoader: ResourceLoader) {
            Player.submitResources(resLoader)
            Robot.submitResources(resLoader)
            ChunkLoader.submitResources(resLoader)
            VisualEffects.submitResources(resLoader)
        }
    }

    val player = Player()
    val chunks = ChunkLoader(
        ChunkLoader.requestChunksFromNetwork(client.network)
    )
    val state = WorldState()
    val vfx = VisualEffects()

    val playerAtCenterCamMode = CameraController.Mode(
        lookAt = { _ -> Vector3f()
            .add(this.player.position)
            .add(0f, +1f, 0f)
        },
        fovDegrees = 30f
    )
    val camController = CameraController(this.playerAtCenterCamMode)

}

fun World.update(client: Client, captureInput: Boolean) {
    this.player.update(client, captureInput)
    this.camController.update(
        client.renderer.camera, client.renderer, captureInput
    )
    this.chunks.update(this.player.position)
    this.state.update(client)
}

fun World.render(client: Client) {
    client.renderer.sunDiameter = 5f + this.camController.distance.value
    val sunTarget = Vector3f(0f, 0f, -this.camController.distance.value * 0.25f)
        .add(this.player.position)
    client.renderer.update(sunTarget)
    client.renderer.forEachPass { pass ->
        this.chunks.render(pass)
        this.state.render(client, pass)
        this.player.render(pass)
        this.vfx.render(pass, client.deltaTime, this.state)
    }
}