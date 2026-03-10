
package schwalbe.ventura.editor

import schwalbe.ventura.client.*
import schwalbe.ventura.client.game.CameraController
import schwalbe.ventura.client.game.ChunkLoader
import schwalbe.ventura.engine.*
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.editor.modes.*
import org.joml.Vector3f
import org.joml.Matrix4f
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class Editor : Application<EditorMode>(
    window = Window(
        name = "Ventura World Editor",
        sizeFactor = 0.9f,
        iconPath = "res/icon.png"
    )
) {

    companion object {
        const val BOOSTED_SPEED_MULTIPLIER: Float = 2f

        const val WORLD_SAVE_DELAY: Long = 500
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
    var saveWorldAfter: Long? = null

}

fun Editor.onWorldEdit() {
    this.saveWorldAfter = System.currentTimeMillis() + Editor.WORLD_SAVE_DELAY
}

private fun Editor.autoSaveWorld() {
    val path: Path = this.worldPath ?: return
    val world: MutableWorld = this.world ?: return
    val saveWorldAfter: Long = this.saveWorldAfter ?: return
    val now: Long = System.currentTimeMillis()
    if (saveWorldAfter > now) { return }
    world.writeToFile(path)
}

private fun Editor.openChosenFile() {
    if (!Key.LEFT_CONTROL.isPressed) { return }
    if (!Key.O.wasPressed) { return }
    try {
        val chooser = JFileChooser()
        chooser.fileFilter = FileNameExtensionFilter("World Data File", "json")
        val status: Int = chooser.showOpenDialog(null)
        if (status != JFileChooser.APPROVE_OPTION) { return }
        val chosenPath: Path = chooser.selectedFile.toPath()
        this.worldPath = chosenPath
        this.world = MutableWorld.readFromFile(chosenPath)
        println("Read world file from '$chosenPath'")
    } catch (e: Exception) {
        e.printStackTrace()
    }
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

private fun Editor.selectMode() {
    if (Key.ESCAPE.wasPressed) {
        this.nav.pop()
        return
    }
    // create local var for current selection
    this.nav.push(when {
        Key.C.wasPressed
            -> return // TODO! deselect, create object mode
        Key.TAB.wasPressed // && selection != null
            -> return // TODO! property editor
        Key.NUM_1.wasPressed // && selection != null
            -> return // TODO! position mode
        Key.NUM_2.wasPressed // && selection != null
            -> return // TODO! rotation mode
        else -> return
    })
}

fun Editor.update() {
    this.autoSaveWorld()
    this.openChosenFile()
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
