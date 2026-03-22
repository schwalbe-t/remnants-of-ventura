
package schwalbe.ventura.editor.modes

import schwalbe.ventura.editor.*
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.data.*
import schwalbe.ventura.utils.SerVector3
import schwalbe.ventura.engine.ui.*
import org.joml.Vector3fc

private fun updateSelectedObjectPosition(editor: Editor): SerVector3? {
    if (!MButton.LEFT.isPressed) { return null }
    val world: LoadedWorld = editor.world ?: return null
    val selected: ObjectInstanceRef = world.selectedObject ?: return null
    val inst = editor.getSelectedObject() ?: return null
    val oldPos: SerVector3 = inst[ObjectProp.Position]
    val newRawPos: Vector3fc = editor.mouseInWorld(
        alignToGrid = Key.LEFT_SHIFT.isPressed,
        y = oldPos.y
    ) ?: return null
    val newPos = SerVector3(newRawPos.x(), oldPos.y, newRawPos.z())
    world.selectedObject = world.withObjectEdit(selected) { ObjectInstance(
        props = it.props
            .filter { p -> p !is ObjectProp.Position }
            .plus(ObjectProp.Position(newPos))
    ) }
    return newPos
}

fun positionMode(editor: Editor): () -> EditorMode = {
    val statusX = Text()
    val statusZ = Text()
    val statusY = Text()
    val mode = EditorMode(
        render = {
            val newPos: SerVector3? = updateSelectedObjectPosition(editor)
            if (newPos != null) {
                statusX.withText("X: ${newPos.x}")
                statusZ.withText("Z: ${newPos.z}")
                statusY.withText("(Y: ${newPos.y})")
            }
            editor.update()
            editor.render()
        },
        navigator = editor.nav
    )
    mode.add(layer = 0, element = Axis.row()
        .add(2f/3f * fpw, Space())
        .add(1f/3f * fpw, createModeDisplay(
            "Position Mode",
            Axis.row(1f/3f * fpw)
                .add(statusX.toModeStatusText())
                .add(statusZ.toModeStatusText())
                .add(statusY.toModeStatusText())
        ))
    )
    mode
}
