
package schwalbe.ventura.editor

import schwalbe.ventura.client.*
import schwalbe.ventura.client.game.CameraController
import schwalbe.ventura.client.game.ChunkLoader
import schwalbe.ventura.engine.*
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.editor.modes.*
import schwalbe.ventura.data.*
import schwalbe.ventura.net.SerVector3
import org.joml.Vector3f
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

data class ObjectInstanceRef(
    val chunk: ChunkRef,
    val instanceIdx: Int
)

class LoadedWorld(val path: Path) {

    val world = MutableWorld.readFromFile(this.path)
    var lastModified: Long? = null
    val chunkLoader = ChunkLoader(
        requestChunks = this::loadChunks,
        loadRadius = 5,
        renderRadius = 5
    )
    var selectedObject: ObjectInstanceRef? = null

    private fun loadChunks(requested: List<ChunkRef>): List<ChunkRef> {
        this.chunkLoader.onChunksReceived(requested.mapNotNull { c ->
            this.world.chunks[c]?.let { c to it.toChunkData() }
        })
        return requested
    }

    fun onEdited() {
        this.lastModified = System.currentTimeMillis()
    }

    fun onChunkEdited(chunk: ChunkRef) {
        this.onEdited()
        this.chunkLoader.invalidateChunk(chunk)
    }

    fun withObjectEdit(
        oldObjRef: ObjectInstanceRef,
        f: (ObjectInstance) -> ObjectInstance
    ): ObjectInstanceRef {
        val oldObj: ObjectInstance = this.world.getChunk(oldObjRef.chunk)
            .instances.removeAt(oldObjRef.instanceIdx)
        this.onChunkEdited(oldObjRef.chunk)
        val newObj: ObjectInstance = f(oldObj)
        val newObjPos: SerVector3 = newObj[ObjectProp.Position]
        val newObjChunkRef = ChunkRef(
            newObjPos.x.unitsToUnitIdx(), newObjPos.z.unitsToUnitIdx()
        )
        val newObjChunk = this.world.getChunk(newObjChunkRef)
        val newObjInstIdx: Int = newObjChunk.instances.size
        newObjChunk.instances.add(newObj)
        this.onChunkEdited(newObjChunkRef)
        val newObjRef = ObjectInstanceRef(newObjChunkRef, newObjInstIdx)
        return newObjRef
    }

}

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


    var world: LoadedWorld? = null

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
    // create local var for current selection
    this.nav.clear(when {
        Key.ESCAPE.wasPressed -> defaultMode(this)
        Key.C.wasPressed -> {
            this.world?.selectedObject = null
            createObjectMode(this)
        }
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
    this.selectMode()
    this.autoSaveWorld()
    this.openChosenFile()
    this.move()
    this.camController.update(
        this.renderer.camera, this.renderer, captureInput = true
    )
    this.world?.chunkLoader?.update(this.position)
}

fun Editor.render() {
    this.renderer.update(this.position)
    this.renderer.forEachPass { pass ->
        this.world?.chunkLoader?.render(pass)
    }
}
