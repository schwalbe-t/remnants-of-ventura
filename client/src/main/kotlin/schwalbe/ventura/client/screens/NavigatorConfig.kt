
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.ui.*
import org.joml.Vector4f

fun configureNavigator(ui: UiNavigator) {
    ui.defaultFont = jetbrainsMonoSb()
    ui.defaultFontSize = 16.px
    ui.defaultFontColor = Vector4f(0.9f, 0.9f, 0.9f, 1f)
}