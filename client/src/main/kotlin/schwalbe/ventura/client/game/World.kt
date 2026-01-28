
package schwalbe.ventura.client.game

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

}

fun World.update(client: Client) {
    this.player.update(client)
    this.camController.update(this.player, client.renderer.camera)
    this.chunks.update(client, this.player.position)
}

fun World.render(client: Client) {
    this.chunks.render(client)
    this.player.render(client)
}

fun renderGameworld(client: Client): () -> Unit = {
    client.renderer.update()
    client.world?.update(client)
    client.world?.render(client)
}
