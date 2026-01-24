
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.ui.*
import org.joml.Vector4f

fun configureNavigator(ui: UiNavigator<GameScreen>) {
    ui.defaultFont = googleSansR()
    ui.defaultFontSize = 16.px
    ui.defaultFontColor = BASE_FONT_COLOR
}