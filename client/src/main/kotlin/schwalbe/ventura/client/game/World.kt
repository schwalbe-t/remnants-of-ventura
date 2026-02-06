
package schwalbe.ventura.client.game

import schwalbe.ventura.engine.*
import schwalbe.ventura.client.Client
import schwalbe.ventura.client.RendererFrag
import schwalbe.ventura.client.RendererVert
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.engine.ui.quad
import org.joml.*

object GroundVert : VertShaderDef<GroundVert> {
    override val path: String = "shaders/ground.vert.glsl"

    val renderer = RendererVert<GroundVert>()
}

object GroundFrag : FragShaderDef<GroundFrag> {
    override val path: String = "shaders/ground.frag.glsl"

    val renderer = RendererFrag<GroundFrag>()
    val groundColor = vec4("uGroundColor")
}

val groundShader: Resource<Shader<GroundVert, GroundFrag>>
    = Shader.loadGlsl(GroundVert, GroundFrag)

class World {

    companion object {
        fun submitResources(resLoader: ResourceLoader) {
            Player.submitResources(resLoader)
            ChunkLoader.submitResources(resLoader)
            resLoader.submit(groundShader)
        }
    }


    val camController = CameraController()
    val player = Player()
    val chunks = ChunkLoader()
    val state = WorldState()

    var groundColor: Vector4fc = Vector4f(0f, 0f, 0f, 0f)

}

fun World.update(client: Client, captureInput: Boolean) {
    this.player.update(client, captureInput)
    this.camController.update(
        client.renderer.camera, client, this, captureInput
    )
    this.chunks.update(client, this.player.position)
    this.state.update(client)
}

private fun World.renderGround(client: Client) {
    val instance = Matrix4f()
        .translate(this.player.position)
        .scale(64f) // diameter
    val shader = groundShader()
    shader[GroundFrag.groundColor] = this.groundColor
    client.renderer.render(
        quad(), shader, GroundVert.renderer, GroundFrag.renderer,
        listOf(instance)
    )
}

fun World.render(client: Client) {
    client.renderer.update()
    client.renderer.dest.clearColor(Vector4f(0.2f, 0.2f, 0.2f, 1.0f))
    this.chunks.render(client)
    this.state.render(client)
    this.player.render(client)
    this.renderGround(client)
}