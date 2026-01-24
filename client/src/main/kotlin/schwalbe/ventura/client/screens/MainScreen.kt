
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*

fun mainScreen(client: Client): UiScreenDef = defineScreen(
    defaultFontColor = BASE_FONT_COLOR
) {
    client.onFrame = renderGridBackground(client)
    val contSize: UiSize = 17.vmin
    it.add(layer = 0, element = Axis.column()
        .add(50.ph - (contSize / 2), Stack())
        .add(contSize, Axis.column()
            .add(5.vmin, createTextButton(
                content = localized()[BUTTON_PLAY],
                handler = {
                    client.nav.push(serverSelectScreen(client))
                }
            ))
            .add(1.vmin, Stack())
            .add(5.vmin, createTextButton(
                content = localized()[BUTTON_CHANGE_LANGUAGE],
                handler = {
                    client.nav.push(languageSelectScreen(client))
                }
            ))
            .add(1.vmin, Stack())
            .add(5.vmin, createTextButton(
                content = localized()[BUTTON_EXIT],
                handler = {
                    client.window.close()
                }
            ))
            .pad(left = 30.pw, right = 30.pw)
        )
        .add(50.ph - (contSize / 2), Stack())
    )
}
