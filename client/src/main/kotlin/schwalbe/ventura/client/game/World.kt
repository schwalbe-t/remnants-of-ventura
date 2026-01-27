
package schwalbe.ventura.client.game

import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector3fc
import schwalbe.ventura.client.Client
import kotlin.math.PI

class World {

    val player = Player()

}

private val cameraEyeOffset: Vector3fc = Vector3f(0f, +2f, +2f)
    .normalize().mul(5f)
private val cameraLookAtOffset: Vector3fc = Vector3f(0f, +1f, 0f)
private const val CAMERA_FOV: Float = PI.toFloat() / 4f // 45 degrees

private fun configureCamera(client: Client) {
    val camera = client.renderer.camera
    val player = client.world?.player ?: return
    camera.position.set(player.position).add(cameraEyeOffset)
    camera.lookAt.set(player.position).add(cameraLookAtOffset)
    camera.fov = CAMERA_FOV
}

fun World.update(client: Client) {
    this.player.update(client)
    configureCamera(client)
}

fun World.render(client: Client) {
    client.renderer.render(
        playerModel(),
        geometryShader(), GeometryVert.renderer, GeometryFrag.renderer,
        this.player.anim, listOf(Matrix4f().scale(Player.MODEL_SCALE))
    )
    this.player.render(client)
}

fun renderGameworld(client: Client): () -> Unit = {
    client.renderer.update()
    client.world?.update(client)
    client.world?.render(client)
}
