
package schwalbe.ventura.editor.modes

import schwalbe.ventura.client.game.ChunkLoader
import schwalbe.ventura.client.projectOnScreen
import schwalbe.ventura.data.ChunkRef
import schwalbe.ventura.editor.*
import schwalbe.ventura.engine.ui.*
import org.joml.*

private class ObjectHandle {

    companion object {
        val SIZE: UiSize = 3.vmin
    }

    val color = FlatBackground()
        .withColor(255, 0, 0, 255)
    val clickArea = ClickArea()
    val offset = Stack()
        .add(this.color)
        .add(this.clickArea)
        .withWidth(SIZE)
        .withHeight(SIZE)
        .pad()
    val root: UiElement = this.offset

    fun update(
        chunkRef: ChunkRef, instIdx: Int,
        inst: ChunkLoader.LoadedInstance,
        world: LoadedWorld, editor: Editor
    ) {
        val worldPos: Vector3fc = inst.transform
            .transformPosition(Vector3f(0f, 0f, 0f))
        val screenPos: Vector2fc = editor.renderer.camera
            .projectOnScreen(editor.window.framebuffer, worldPos)
        this.offset.withPadding(
            left = screenPos.x().px - (SIZE / 2),
            top = screenPos.y().px - (SIZE / 2),
            right = 0.px, bottom = 0.px
        )
        val selectedObject: ObjectInstanceRef? = world.selectedObject
        val isSelected: Boolean = selectedObject != null
            && selectedObject.chunk == chunkRef
            && selectedObject.instanceIdx == instIdx
        this.color
            .withColor(if (isSelected) SELECTED_COLOR else BACKGROUND_COLOR)
            .withHoverColor(if (isSelected) SELECTED_COLOR else HOVER_COLOR)
        this.clickArea.onLeftClick = {
            world.selectedObject = ObjectInstanceRef(chunkRef, instIdx)
        }
    }

}

fun defaultMode(editor: Editor): () -> EditorMode = {
    val handleContainer = Stack()
    val allHandles = mutableMapOf<ChunkRef, MutableList<ObjectHandle>>()
    fun updateHandles() {
        val world: LoadedWorld = editor.world ?: return
        for ((chunkRef, handles) in allHandles.toList()) {
            if (chunkRef in world.chunkLoader.loaded) { continue }
            handles.forEach { handleContainer.dispose(it.root) }
            allHandles.remove(chunkRef)
        }
        for ((chunkRef, chunk) in world.chunkLoader.loaded) {
            val handles = allHandles.getOrPut(chunkRef) { mutableListOf() }
            if (handles.size > chunk.instances.size) {
                val removed = handles.subList(0, chunk.instances.size)
                removed.forEach { handleContainer.dispose(it.root) }
                removed.clear()
            }
            (handles.size..<chunk.instances.size).forEach { _ ->
                val added = ObjectHandle()
                handleContainer.add(added.root)
                handles.add(added)
            }
            for (i in handles.indices) {
                handles[i].update(
                    chunkRef, i, chunk.instances[i], world, editor
                )
            }
        }
    }
    val mode = EditorMode(
        render = {
            editor.update()
            updateHandles()
            editor.render()
        },
        navigator = editor.nav
    )
    mode.add(layer = 0, element = handleContainer)
    mode
}
