
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.gfx.*

val quad: Resource<Geometry> = Resource { {
    val layout = listOf(Geometry.float(2))
    val vertices: FloatArray = floatArrayOf(
        0f, 1f, // [0] top left
        1f, 1f, // [1] top right
        0f, 0f, // [2] bottom left
        1f, 0f  // [3] bottom right
    )
    val elements: ShortArray = shortArrayOf(
        0, 2, 3,
        0, 3, 1
    )
    Geometry.fromFloatArray(layout, vertices, elements)
} }

val blitShader: Resource<Shader<PxPos, Blit>>
    = Shader.loadGlsl(PxPos, Blit)
val flatBgShader: Resource<Shader<FullBuffer, FlatBg>>
    = Shader.loadGlsl(FullBuffer, FlatBg)
val fillColorShader: Resource<Shader<PxPos, FlatBg>>
    = Shader.loadGlsl(PxPos, FlatBg)
val gaussianDistribShader: Resource<Shader<FullBuffer, GaussianDistrib>>
    = Shader.loadGlsl(FullBuffer, GaussianDistrib)
val blurBgShader: Resource<Shader<FullBuffer, BlurBg>>
    = Shader.loadGlsl(FullBuffer, BlurBg)
val roundedBlitShader: Resource<Shader<FullBuffer, RoundedBlit>>
    = Shader.loadGlsl(FullBuffer, RoundedBlit)
    
fun loadUiResources(loader: ResourceLoader): Unit = loader.submitAll(
    quad,
    blitShader,
    flatBgShader, fillColorShader,
    gaussianDistribShader, blurBgShader,
    roundedBlitShader
)
