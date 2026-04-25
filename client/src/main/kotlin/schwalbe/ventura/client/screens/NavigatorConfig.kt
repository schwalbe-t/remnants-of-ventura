
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.ui.*

fun loadNavigatorResources(loader: ResourceLoader) = loader.submitAll(
    googleSansR
)

fun configureNavigator(ui: UiNavigator<GameScreen>) {
    ui.defaultFont = googleSansR()
    ui.defaultFontSize = 16.px
    ui.defaultFontColor = Theme.FONT_COLOR
}