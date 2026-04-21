
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.ui.Font
import schwalbe.ventura.engine.ui.loadTtf
import org.joml.Vector3f
import schwalbe.ventura.client.Camera
import kotlin.math.PI

// Note: Names of font files must be preserved 1:1 in code files.
//       The build script searches through the source code for font names
//       to filter out unused fonts.

val jetbrainsMonoSb: Resource<Font>
    = Font.loadTtf("res/fonts/JetBrainsMonoNL-SemiBold.ttf")
val jetbrainsMonoB: Resource<Font>
        = Font.loadTtf("res/fonts/JetBrainsMonoNL-Bold.ttf")
val jetbrainsMonoEB: Resource<Font>
        = Font.loadTtf("res/fonts/JetBrainsMonoNL-ExtraBold.ttf")
val jetbrainsMonoI: Resource<Font>
    = Font.loadTtf("res/fonts/JetBrainsMonoNL-Italic.ttf")

val googleSansR: Resource<Font>
    = Font.loadTtf("res/fonts/GoogleSans-Regular.ttf")
val googleSansSb: Resource<Font>
    = Font.loadTtf("res/fonts/GoogleSans-SemiBold.ttf")
val googleSansI: Resource<Font>
    = Font.loadTtf("res/fonts/GoogleSans-Italic.ttf")

val backgroundWorld = Resource(allowReset = true) { {
    WorldBackground.World(
        worldFile = "res/worlds/main_menu.json",
        camera = Camera(
            position = Vector3f(0f, +15f, +25f),
            lookAt = Vector3f(0f, 0f, 0f),
            fov = PI.toFloat() / 9f // 180/9 = 20 deg
        ),
        triggered = setOf("nor_lamp")
    )
} }

fun submitScreenResources(loader: ResourceLoader) {
    loader.submitAll(
        jetbrainsMonoSb, jetbrainsMonoB, jetbrainsMonoEB, jetbrainsMonoI,
        googleSansR, googleSansSb, googleSansI,
        backgroundWorld
    )
    Icons.submit(loader)
}
