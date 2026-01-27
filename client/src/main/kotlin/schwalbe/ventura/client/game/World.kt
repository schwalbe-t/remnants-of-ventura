
package schwalbe.ventura.client.game

import org.joml.Matrix4f
import schwalbe.ventura.client.Client

class World {

    val player = Player()

}

fun World.update(client: Client) {
    this.player.update(client)
}

fun World.render(client: Client) {
    client.renderer.render(
        playerModel(),
        geometryShader(), GeometryVert.renderer, GeometryFrag.renderer,
        null, listOf(Matrix4f().scale(Player.MODEL_SCALE))
    )
    this.player.render(client)
}

fun renderGameworld(client: Client): () -> Unit = {
    client.renderer.update()
    client.world?.update(client)
    client.world?.render(client)
}
