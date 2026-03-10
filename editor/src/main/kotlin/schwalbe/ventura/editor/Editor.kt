
package schwalbe.ventura.editor

import org.joml.Matrix4f
import schwalbe.ventura.client.*
import schwalbe.ventura.client.game.CameraController
import schwalbe.ventura.engine.*
import schwalbe.ventura.client.game.ChunkLoader
import schwalbe.ventura.engine.input.Key
import schwalbe.ventura.engine.input.isPressed
import org.joml.Vector3f
import java.nio.file.Path

class Editor : Application<EditorScreen>(
    window = Window(
        name = "Ventura World Editor",
        sizeFactor = 0.9f,
        iconPath = "res/icon.png"
    )
) {

    companion object {
        const val BOOSTED_SPEED_MULTIPLIER: Float = 2f
    }


    init {
        this.window.setVsyncEnabled(true)
    }

    val renderer = Renderer(
        this.out3d,
        camera = Camera(far = 500f)
    )


    var position = Vector3f()
    val cameraMode = CameraController.Mode(
        lookAt = { _ -> this.position },
        fovDegrees = 30f
    )
    val camController = CameraController(
        this.cameraMode, minDist = 5f, maxDist = 100f
    )


    var worldPath: Path? = null
    var world: MutableWorld? = null

}

fun Editor.saveWorld() {
    val path: Path = this.worldPath ?: return
    val world: MutableWorld = this.world ?: return
    world.writeToFile(path)
}

private fun Editor.move() {
    val velocity = Vector3f()
    if (Key.A.isPressed) { velocity.x -= 1f }
    if (Key.D.isPressed) { velocity.x += 1f }
    if (Key.W.isPressed) { velocity.z -= 1f }
    if (Key.S.isPressed) { velocity.z += 1f }
    if (velocity.lengthSquared() == 0f) { return }
    velocity.normalize()
        .mul(this.camController.userDistance)
        .mul(this.deltaTime)
    if (Key.LEFT_CONTROL.isPressed) {
        velocity.mul(Editor.BOOSTED_SPEED_MULTIPLIER)
    }
    this.position.add(velocity)
}

fun Editor.update() {
    this.move()
    this.camController.update(
        this.renderer.camera, this.renderer, captureInput = true
    )
}

fun Editor.render() {
    this.renderer.update(this.position)
    this.renderer.forEachPass { pass ->
        val model = ChunkLoader.objectModels[0]()
        pass.renderGeometry(model, null, listOf(Matrix4f()))
    }
}
