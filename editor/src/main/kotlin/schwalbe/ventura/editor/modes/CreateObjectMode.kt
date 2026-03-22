
package schwalbe.ventura.editor.modes

import schwalbe.ventura.data.ObjectType
import schwalbe.ventura.editor.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.data.*
import schwalbe.ventura.utils.toSerVector3
import org.joml.Vector3f

private class ObjectSelector(
    val type: ObjectType,
    onSelect: (ObjectSelector) -> Unit
) {
    companion object {
        val HEIGHT: UiSize = 3.5.vmin
    }

    val background = FlatBackground()
        .withColor(BACKGROUND_COLOR)
        .withHoverColor(HOVER_COLOR)
    val root = Stack()
        .add(this.background)
        .add(Text()
            .withText(this.type.name)
            .withSize(70.ph)
            .pad(0.75.vmin)
        )
        .add(ClickArea().withLeftHandler { onSelect(this) })
}

private fun Editor.placeSelectedObject(selectedType: ObjectType) {
    if (!MButton.LEFT.wasPressed) { return }
    val world: LoadedWorld = this.world ?: return
    val position: Vector3f = this
        .mouseInWorld(alignToGrid = Key.LEFT_SHIFT.isPressed)
        ?: return
    val chunkRef = ChunkRef(
        position.x.unitsToChunkIdx(), position.z.unitsToChunkIdx()
    )
    val chunk: MutableChunkData = world.world.getChunk(chunkRef)
    chunk.instances.add(ObjectInstance(listOf(
        ObjectProp.Type(selectedType),
        ObjectProp.Position(position.toSerVector3())
    )))
    world.onChunkEdited(chunkRef)
}

fun createObjectMode(editor: Editor): () -> EditorMode = {
    var selected: ObjectSelector? = null
    val selectedText = Text()
    val objectList: SizedAxis = Axis.column(ObjectSelector.HEIGHT)
    for (type in ObjectType.entries) {
        val selector = ObjectSelector(type) { s ->
            selectedText.withText(s.type.name)
            selected?.background
                ?.withColor(TRANSPARENT)
                ?.withHoverColor(HOVER_COLOR)
            s.background
                .withColor(SELECTED_COLOR)
                .withHoverColor(SELECTED_COLOR)
            selected = s
        }
        objectList.add(selector.root)
    }
    objectList.add(50.ph, Space())
    val mode = EditorMode(
        render = {
            selected?.let {
                editor.placeSelectedObject(it.type)
            }
            editor.update()
            editor.render()
        },
        navigator = editor.nav
    )
    mode.add(layer = 0, element = Axis.row()
        .add(1f/5f * fpw, Stack()
            .add(FlatBackground().withColor(BACKGROUND_COLOR))
            .add(objectList
                .wrapScrolling(horiz = false, vert = true)
            )
        )
        .add((2f/3f - 1/5f) * fpw, Space())
        .add(1/3f * fpw, createModeDisplay(
            "Object Creation Mode",
            selectedText.toModeStatusText()
                .alignRight()
        ))
    )
    mode
}
