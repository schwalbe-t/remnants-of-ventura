
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.gfx.Shader
import schwalbe.ventura.engine.gfx.loadGlsl
import schwalbe.ventura.engine.ui.Font
import schwalbe.ventura.engine.ui.loadTtf

private fun fontPathOf(name: String, variant: String): String
    = "res/fonts/$name-$variant.ttf"

val jetbrainsMonoSb: Resource<Font>
    = Font.loadTtf(fontPathOf("JetBrainsMonoNL", "SemiBold"))
val jetbrainsMonoB: Resource<Font>
    = Font.loadTtf(fontPathOf("JetBrainsMonoNL", "Bold"))

val googleSansR: Resource<Font>
    = Font.loadTtf(fontPathOf("GoogleSans", "Regular"))
val googleSansSb: Resource<Font>
    = Font.loadTtf(fontPathOf("GoogleSans", "SemiBold"))
val googleSansI: Resource<Font>
    = Font.loadTtf(fontPathOf("GoogleSans", "Italic"))

val gridShader: Resource<Shader<GridVert, GridFrag>>
    = Shader.loadGlsl(GridVert, GridFrag)

fun submitScreenResources(loader: ResourceLoader) = loader.submitAll(
    jetbrainsMonoSb, jetbrainsMonoB,
    googleSansR, googleSansSb, googleSansI,
    gridShader, gridQuad
)
