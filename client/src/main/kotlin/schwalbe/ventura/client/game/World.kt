
package schwalbe.ventura.client.game

import org.joml.Vector4f
import schwalbe.ventura.engine.*
import schwalbe.ventura.client.Client

class World {

    companion object {
        fun submitResources(resLoader: ResourceLoader) {
            Player.submitResources(resLoader)
            ChunkLoader.submitResources(resLoader)
        }
    }


    val camController = CameraController()
    val player = Player()
    val chunks = ChunkLoader()
    val state = WorldState()

}

fun World.update(client: Client, captureInput: Boolean) {
    this.player.update(client, captureInput)
    this.camController.update(
        client.renderer.camera, client, this, captureInput
    )
    this.chunks.update(client, this.player.position)
    this.state.update(client)
}

fun World.render(client: Client) {
    client.renderer.update()
    client.renderer.dest.clearColor(Vector4f(151f, 134f, 111f, 255f).div(255f))
    this.chunks.render(client)
    this.state.render(client)
    this.player.render(client)
}