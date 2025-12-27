
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.gfx.*

val quad: Resource<Geometry> = Resource { {
    val layout = listOf(Geometry.Attribute(2, Geometry.Type.FLOAT))
    val vertices: FloatArray = floatArrayOf(
        0f, 1f, // top left
        1f, 1f, // top right
        0f, 0f, // bottom left
        1f, 0f  // bottom right
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

fun loadUiResources(loader: ResourceLoader): Unit = loader.submitAll(
    quad,
    blitShader,
    flatBgShader
)
