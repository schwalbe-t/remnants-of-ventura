
package schwalbe.ventura.editor.modes

import schwalbe.ventura.client.game.ChunkLoader
import schwalbe.ventura.client.projectOnScreen
import schwalbe.ventura.data.ChunkRef
import schwalbe.ventura.editor.*
import schwalbe.ventura.engine.ui.*
import org.joml.*
import schwalbe.ventura.engine.input.Key
import schwalbe.ventura.engine.input.wasPressed

private class ObjectHandle {

    companion object {
        val SIZE: UiSize = 3.vmin
    }

    var chunkRef: ChunkRef? = null
    var instanceIdx: Int? = null
    var world: LoadedWorld? = null

    val color = FlatBackground()
        .withColor(255, 0, 0, 255)
    val offset = Stack()
        .add(this.color)
        .add(ClickArea().withLeftHandler {
            this.world?.selectedObject = ObjectInstanceRef(
                this.chunkRef ?: return@withLeftHandler,
                this.instanceIdx ?: return@withLeftHandler
            )
        })
        .withWidth(SIZE)
        .withHeight(SIZE)
        .pad()
    val root: UiElement = this.offset

    fun update(inst: ChunkLoader.LoadedInstance, editor: Editor) {
        val worldPos: Vector3fc = inst.transform
            .transformPosition(Vector3f(0f, 0f, 0f))
        val screenPos: Vector2fc = editor.renderer.camera
            .projectOnScreen(editor.window.framebuffer, worldPos)
        this.offset.withPadding(
            left = screenPos.x().px - (SIZE / 2),
            top = screenPos.y().px - (SIZE / 2),
            right = 0.px, bottom = 0.px
        )
        val selectedObject: ObjectInstanceRef? = this.world?.selectedObject
        val isSelected: Boolean = selectedObject != null
            && selectedObject.chunk == this.chunkRef
            && selectedObject.instanceIdx == this.instanceIdx
        this.color
            .withColor(if (isSelected) SELECTED_COLOR else BACKGROUND_COLOR)
            .withHoverColor(if (isSelected) SELECTED_COLOR else HOVER_COLOR)
    }

}

fun defaultMode(editor: Editor): () -> EditorMode = {
    val handleContainer = Stack()
    val allHandles = mutableMapOf<ChunkRef, MutableList<ObjectHandle>>()
    fun updateHandles(world: LoadedWorld) {
        for ((chunkRef, handles) in allHandles.toList()) {
            if (chunkRef in world.chunkLoader.loaded) { continue }
            handles.forEach { handleContainer.dispose(it.root) }
            allHandles.remove(chunkRef)
        }
        for ((chunkRef, chunk) in world.chunkLoader.loaded) {
            val handles = allHandles.getOrPut(chunkRef) { mutableListOf() }
            if (handles.size > chunk.instances.size) {
                val rem = handles.subList(chunk.instances.size, handles.size)
                rem.forEach { handleContainer.dispose(it.root) }
                rem.clear()
            }
            (handles.size..<chunk.instances.size).forEach { _ ->
                val added = ObjectHandle()
                handleContainer.add(added.root)
                handles.add(added)
            }
            for ((i, handle) in handles.withIndex()) {
                handle.chunkRef = chunkRef
                handle.instanceIdx = i
                handle.world = world
                handle.update(chunk.instances[i], editor)
            }
        }
    }
    val mode = EditorMode(
        render = {
            val world: LoadedWorld? = editor.world
            val selectedObject: ObjectInstanceRef? = world?.selectedObject
            val deleteSelected: Boolean = world != null
                && selectedObject != null
                && (Key.DELETE.wasPressed || Key.BACKSPACE.wasPressed)
            if (deleteSelected) {
                val chunk = world.world.getChunk(selectedObject.chunk)
                chunk.instances.removeAt(selectedObject.instanceIdx)
                world.onChunkEdited(selectedObject.chunk)
                world.selectedObject = null
            }
            editor.update()
            if (world != null) {
                updateHandles(world)
            }
            editor.render()
        },
        navigator = editor.nav
    )
    mode.add(layer = 0, element = handleContainer)
    mode.add(layer = 1, element = Axis.row()
        .add(2f/3f * fpw, Space())
        .add(1f/3f * fpw, createModeDisplay("Selection Mode", Space()))
    )
    mode
}
