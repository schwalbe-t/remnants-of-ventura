
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.VertShaderDef
import schwalbe.ventura.engine.gfx.FragShaderDef

object PxPos : VertShaderDef<PxPos> {
    override val path = "shaders/ui/px_pos.vert.glsl"

    val bufferSizePx = vec2("uBufferSizePx")
    val destTopLeftPx = vec2("uDestTopLeftPx")
    val destSizePx = vec2("uDestSizePx")
}

object FullBuffer : VertShaderDef<FullBuffer> {
    override val path = "shaders/ui/full_buffer.vert.glsl"
}


object Blit : FragShaderDef<Blit> {
    override val path = "shaders/ui/blit.frag.glsl"
    
    val texture = sampler2D("uTexture")
}

object FlatBg : FragShaderDef<FlatBg> {
    override val path = "shaders/ui/flat_bg.frag.glsl"
    
    val color = vec4("uColor")
}