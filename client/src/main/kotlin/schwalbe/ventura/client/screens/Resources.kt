
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.ui.Font
import schwalbe.ventura.engine.ui.loadTtf

val jetbrainsMonoSb: Resource<Font> = Font.loadTtf(
    "res/fonts/JetBrainsMonoNL-SemiBold.ttf"
)
val jetbrainsMonoB: Resource<Font> = Font.loadTtf(
    "res/fonts/JetBrainsMonoNL-Bold.ttf"
)

fun submitScreenResources(loader: ResourceLoader) = loader.submitAll(
    jetbrainsMonoSb, jetbrainsMonoB
)
