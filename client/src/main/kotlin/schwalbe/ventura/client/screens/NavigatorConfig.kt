
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.ui.*

fun configureNavigator(ui: UiNavigator<GameScreen>) {
    ui.defaultFont = googleSansR()
    ui.defaultFontSize = 16.px
    ui.defaultFontColor = Theme.FONT_COLOR
}