
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

object GaussianDistrib : FragShaderDef<GaussianDistrib> {
    override val path = "shaders/ui/gaussian_distrib.frag.glsl"
    
    val sigma = float("uSigma")
    val kernelRadius = int("uKernelRadius")
}

object BlurBg : FragShaderDef<BlurBg> {
    override val path = "shaders/ui/blur_bg.frag.glsl"
    
    val background = sampler2D("uBackground")
    val backgroundSizePx = vec2("uBackgroundSizePx")
    val bufferSizePx = vec2("uBufferSizePx")
    val absOffsetPx = vec2("uAbsOffsetPx")
    
    val kernelRadius = int("uKernelRadius")
    val kernelWeights = sampler2D("uKernelWeights")
}