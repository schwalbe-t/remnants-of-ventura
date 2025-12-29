
package schwalbe.ventura.engine.ui

import org.joml.Vector2f
import org.joml.Vector2fc
import schwalbe.ventura.engine.gfx.ConstFramebuffer
import schwalbe.ventura.engine.gfx.DepthTesting
import schwalbe.ventura.engine.gfx.Shader
import schwalbe.ventura.engine.gfx.Texture

fun blitTexture(
    texture: Texture?, dest: ConstFramebuffer,
    destPos: Vector2fc, destSize: Vector2fc
) {
    if (texture == null) { return }
    val shader: Shader<PxPos, Blit> = blitShader()
    shader[PxPos.bufferSizePx] = Vector2f(
        dest.width.toFloat(), dest.height.toFloat()
    )
    shader[PxPos.destTopLeftPx] = destPos
    shader[PxPos.destSizePx] = destSize
    shader[Blit.texture] = texture
    quad().render(shader, dest, depthTesting = DepthTesting.DISABLED)
}

fun blitTexture(
    texture: Texture?, dest: ConstFramebuffer,
    destX: Int = 0, destY: Int = 0,
    destW: Int = dest.width, destH: Int = dest.height
) = blitTexture(
    texture, dest,
    Vector2f(destX.toFloat(), destY.toFloat()),
    Vector2f(destW.toFloat(), destH.toFloat())
)