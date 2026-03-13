
package schwalbe.ventura.editor.modes

import schwalbe.ventura.client.castRay
import schwalbe.ventura.client.intersectXZ
import schwalbe.ventura.editor.Editor
import schwalbe.ventura.engine.input.Mouse
import schwalbe.ventura.engine.ui.*
import org.joml.*

val TRANSPARENT: Vector4fc = Vector4f(0f, 0f, 0f, 0f)
val BACKGROUND_COLOR: Vector4fc = Vector4f(0.9f, 0.9f, 0.9f, 1f)
val HOVER_COLOR: Vector4fc = Vector4f(0.7f, 0.7f, 0.9f, 1f)
val SELECTED_COLOR: Vector4fc = Vector4f(0.5f, 0.5f, 0.9f, 1f)

const val POSITION_GRID_SIZE: Float = 1f

fun Editor.mouseInWorld(alignToGrid: Boolean, y: Float = 0f): Vector3f? {
    val raw: Vector3f = this.renderer.camera
        .castRay(this.window.framebuffer, Mouse.position)
        .intersectXZ(y)
        ?: return null
    if (!alignToGrid) { return raw }
    return raw.div(POSITION_GRID_SIZE).round().mul(POSITION_GRID_SIZE)
}

val STATUS_TEXT_HEIGHT: UiSize = 3.vmin

fun Text.toModeStatusText(): Text = this
    .withColor(BACKGROUND_COLOR)
    .withSize(70.ph)

fun createModeDisplay(modeName: String, status: UiElement) = Axis.column()
    .add(STATUS_TEXT_HEIGHT, Text()
        .withText(modeName)
        .alignRight()
        .toModeStatusText()
    )
    .add(100.ph - (2 * STATUS_TEXT_HEIGHT), Space())
    .add(STATUS_TEXT_HEIGHT, status)
    .pad(0.5.vmin)
