
package schwalbe.ventura.editor.modes

import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.Vector4fc
import schwalbe.ventura.client.castRay
import schwalbe.ventura.client.intersectXZ
import schwalbe.ventura.editor.Editor
import schwalbe.ventura.engine.input.Mouse

val BACKGROUND_COLOR: Vector4fc = Vector4f(0f, 0f, 0f, 0f)
val HOVER_COLOR: Vector4fc = Vector4f(0.7f, 0.7f, 0.9f, 1f)
val SELECTED_COLOR: Vector4fc = Vector4f(0.5f, 0.5f, 0.9f, 1f)

const val POSITION_GRID_SIZE: Float = 1f

fun Editor.mouseInWorld(alignToGrid: Boolean): Vector3f? {
    val raw: Vector3f = this.renderer.camera
        .castRay(this.window.framebuffer, Mouse.position)
        .intersectXZ(y = 0f)
        ?: return null
    if (!alignToGrid) { return raw }
    return raw.div(POSITION_GRID_SIZE).round().mul(POSITION_GRID_SIZE)
}
