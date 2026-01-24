
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.gfx.Shader
import schwalbe.ventura.engine.gfx.loadGlsl
import schwalbe.ventura.engine.ui.Font
import schwalbe.ventura.engine.ui.loadTtf

val jetbrainsMonoSb: Resource<Font> = Font.loadTtf(
    "res/fonts/JetBrainsMonoNL-SemiBold.ttf"
)
val jetbrainsMonoB: Resource<Font> = Font.loadTtf(
    "res/fonts/JetBrainsMonoNL-Bold.ttf"
)

val googleSansR: Resource<Font> = Font.loadTtf(
    "res/fonts/GoogleSans-Regular.ttf"
)
val googleSansSb: Resource<Font> = Font.loadTtf(
    "res/fonts/GoogleSans-SemiBold.ttf"
)

val gridShader: Resource<Shader<GridVert, GridFrag>>
    = Shader.loadGlsl(GridVert, GridFrag)

fun submitScreenResources(loader: ResourceLoader) = loader.submitAll(
    jetbrainsMonoSb, jetbrainsMonoB,
    googleSansR, googleSansSb,
    gridShader, gridQuad
)
