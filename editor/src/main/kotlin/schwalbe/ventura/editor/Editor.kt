
package schwalbe.ventura.editor

import schwalbe.ventura.client.*
import schwalbe.ventura.client.game.CameraController
import schwalbe.ventura.client.game.ChunkLoader
import schwalbe.ventura.engine.*
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.editor.modes.*
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.data.*
import org.joml.Vector3f
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private val whiteOutlineShader: Resource<Shader<OutlineVert, OutlineFrag>>
    = Shader.loadGlsl(OutlineVert, OutlineFrag, macros = mapOf(
        "OUTLINE_COLOR_OVERRIDE" to "vec4(0.5, 0.5, 0.9, 1.0)"
    ))

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

        fun submitResources(loader: ResourceLoader) = loader.submitAll(
            whiteOutlineShader
        )
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
        this.cameraMode,
        minDist = 5f, maxDist = 100f,
        startDist = 100f
    )


    var world: LoadedWorld? = null

}

fun Editor.getSelectedObject(): ObjectInstance? {
    val world: LoadedWorld = this.world ?: return null
    val selectedRef: ObjectInstanceRef = world.selectedObject ?: return null
    val chunk: MutableChunkData = world.world.getChunk(selectedRef.chunk)
    return chunk.instances[selectedRef.instanceIdx]
}

private fun Editor.autoSaveWorld() {
    val world: LoadedWorld = this.world ?: return
    val lastModified: Long = world.lastModified ?: return
    val now: Long = System.currentTimeMillis()
    if (lastModified + Editor.WORLD_SAVE_DELAY > now) { return }
    world.world.writeToFile(world.path)
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
        this.world = LoadedWorld(chosenPath)
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
    val hasSelection: Boolean = this.world?.selectedObject != null
    this.nav.clear(when {
        Key.ESCAPE.wasPressed -> defaultMode(this)
        Key.C.wasPressed -> {
            this.world?.selectedObject = null
            createObjectMode(this)
        }
        Key.TAB.wasPressed && hasSelection
            -> propEditMode(this)
        Key.NUM_1.wasPressed && hasSelection
            -> positionMode(this)
        Key.NUM_2.wasPressed && hasSelection
            -> rotationMode(this)
        else -> return
    })
}

fun Editor.update() {
    this.selectMode()
    this.autoSaveWorld()
    this.openChosenFile()
    this.move()
    this.camController.update(
        this.renderer.camera, this.renderer, captureInput = true
    )
    this.world?.chunkLoader?.update(this.position)
}

private fun Editor.renderSelectedObject(pass: RenderPass) {
    val world: LoadedWorld = this.world ?: return
    val objectRef: ObjectInstanceRef = world.selectedObject ?: return
    val chunk = world.chunkLoader.loaded[objectRef.chunk] ?: return
    val inst = chunk.instances[objectRef.instanceIdx]
    val instType: ObjectType = inst.obj[ObjectProp.Type]
    val model: Model<StaticAnim> = ChunkLoader.objectModels
        .getOrNull(instType.ordinal)?.invoke() ?: return
    val shader = whiteOutlineShader()
    shader[OutlineVert.outlineThickness] = 0.25f
    pass.render(
        model, shader, OutlineVert.renderer, OutlineFrag.renderer,
        animState = null, listOf(inst.transform),
        FaceCulling.FRONT
    )
}

fun Editor.render() {
    this.renderer.update(this.position)
    this.renderer.forEachPass { pass ->
        this.world?.chunkLoader?.render(pass)
        this.renderSelectedObject(pass)
    }
}
