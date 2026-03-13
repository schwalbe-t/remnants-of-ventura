
package schwalbe.ventura.editor.modes

import org.joml.Vector2f
import org.joml.Vector2fc
import schwalbe.ventura.editor.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.data.ObjectInstance
import schwalbe.ventura.data.ObjectProp
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.net.SerVector3
import kotlin.math.PI
import kotlin.math.roundToInt

private fun modifyRotation(
    editor: Editor, f: (SerVector3) -> SerVector3
): SerVector3? {
    val world: LoadedWorld = editor.world ?: return null
    val selected: ObjectInstanceRef = world.selectedObject ?: return null
    val inst = editor.getSelectedObject() ?: return null
    val oldRotation: SerVector3 = inst[ObjectProp.Rotation]
    val newRotation: SerVector3 = f(oldRotation)
    world.selectedObject = world.withObjectEdit(selected) { ObjectInstance(
        props = it.props
            .filter { p -> p !is ObjectProp.Rotation }
            .plus(ObjectProp.Rotation(newRotation))
    ) }
    return newRotation
}

private class AnchoredMouse {
    var pos: Vector2f? = null
    var angle: Float? = null
}

private const val DEGREE: Double = PI / 180.0

private fun updateSelectedObjectRotation(
    editor: Editor, anchor: AnchoredMouse
): SerVector3? {
    if (MButton.LEFT.isPressed) {
        val currRot: Float = editor.getSelectedObject()
            ?.get(ObjectProp.Rotation)?.y ?: 0f
        val anchorPos: Vector2f = anchor.pos ?: Vector2f(Mouse.position)
        val anchorAngle: Float = anchor.angle ?: currRot
        anchor.pos = anchorPos
        anchor.angle = anchorAngle
        val posDiffX: Float = Mouse.position.x() - anchorPos.x()
        val anglePerPixel: Double = 2 * PI / editor.window.framebuffer.width
        val angleDiffY: Float = (posDiffX * anglePerPixel).toFloat()
        return modifyRotation(editor) {
            var newY = anchorAngle + angleDiffY
            if (Key.LEFT_SHIFT.isPressed) {
                val align: Double = 5 * DEGREE
                newY = (newY / align).roundToInt() * align.toFloat()
            }
            SerVector3(it.x, newY, it.z)
        }
    } else {
        anchor.pos = null
        anchor.angle = null
    }
    if (Key.LEFT.wasPressed) {
        return modifyRotation(editor) {
            val newY = ((it.y / DEGREE).roundToInt() + 1) * DEGREE.toFloat()
            SerVector3(it.x, newY, it.z)
        }
    }
    if (Key.RIGHT.wasPressed) {
        return modifyRotation(editor) {
            val newY = ((it.y / DEGREE).roundToInt() - 1) * DEGREE.toFloat()
            SerVector3(it.x, newY, it.z)
        }
    }
    if (Key.R.wasPressed) {
        return modifyRotation(editor) {
            val newY = (Math.random() * 2.0 * PI).toFloat()
            SerVector3(it.x, newY, it.z)
        }
    }
    return null
}

private fun Text.withAngleText(label: String, angleRad: Float): Text {
    val rounded = (angleRad / DEGREE * 100.0).roundToInt() / 100.0
    return this.withText("$label: $rounded°")
}

fun rotationMode(editor: Editor): () -> EditorMode = {
    val statusX = Text()
    val statusY = Text()
    val statusZ = Text()
    val anchor = AnchoredMouse()
    val mode = EditorMode(
        render = {
            val newRot = updateSelectedObjectRotation(editor, anchor)
            if (newRot != null) {
                statusX.withAngleText("X", newRot.x)
                statusY.withAngleText("Y", newRot.y)
                statusZ.withAngleText("Z", newRot.z)
            }
            editor.update()
            editor.render()
        },
        navigator = editor.nav
    )
    mode.add(layer = 0, element = Axis.row()
        .add(2f/3f * fpw, Space())
        .add(1f/3f * fpw, createModeDisplay(
            "Rotation Mode",
            Axis.row(1f/3f * fpw)
                .add(statusX.toModeStatusText())
                .add(statusY.toModeStatusText())
                .add(statusZ.toModeStatusText())
        ))
    )
    mode
}
