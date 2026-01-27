
package schwalbe.ventura.client.game

import org.joml.Matrix4f
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.engine.*
import schwalbe.ventura.client.Client
import schwalbe.ventura.worlds.ObjectType


val objectModels: List<Resource<Model<StaticAnim>>> = ObjectType.entries
    .map {
        Model.loadFile(
            it.filePath,
            Renderer.meshProperties,
            textureFilter = Texture.Filter.LINEAR
        )
    }


class World {

    companion object {
        fun submitResources(loader: ResourceLoader) {
            objectModels.forEach(loader::submit)
        }
    }


    val camController = CameraController()
    val player = Player()

}

fun World.update(client: Client) {
    this.player.update(client)
    this.camController.update(this.player, client.renderer.camera)
}

fun World.render(client: Client) {
    val rockModel = objectModels[ObjectType.ROCK.ordinal]()
    val rockInstances = listOf(Matrix4f().rotateY(0.5f))
    client.renderer.renderOutline(rockModel, 0.015f, null, rockInstances)
    client.renderer.renderGeometry(rockModel, null, rockInstances)
    this.player.render(client)
}

fun renderGameworld(client: Client): () -> Unit = {
    client.renderer.update()
    client.world?.update(client)
    client.world?.render(client)
}
