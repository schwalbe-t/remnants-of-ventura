
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*

fun serverConnectingScreen(
    name: String, client: Client
): UiScreenDef = defineScreen(
    defaultFontColor = BASE_FONT_COLOR
) {
    it.add(layer = -1, element = FlatBackground()
        .withColor(BACKGROUND_COLOR)
    )
    val contSize: UiSize = 20.vmin
    it.add(layer = 0, element = Axis.column()
        .add(50.ph - (contSize / 2), Stack())
        .add(contSize, Axis.column()
            .add(5.vmin, Text()
                .withText(localized()[TITLE_CONNECTING_TO_SERVER])
                .withSize(70.ph)
                .withAlignment(Text.Alignment.CENTER)
            )
            .add(3.vmin, Text()
                .withText(name)
                .withColor(DARK_FONT_COLOR)
                .withSize(70.ph)
                .withAlignment(Text.Alignment.CENTER)
            )
            .add(7.vmin, Stack())
            .add(5.vmin,
                createTextButton(
                    content = localized()[BUTTON_CANCEL_CONNECTION],
                    handler = {
                        // TODO! cancel connection to server (logic in 'Client')
                        client.nav.clear(serverSelectScreen(client))
                    }
                ).pad(left = 30.pw, right = 30.pw)
            )
        )
        .add(50.ph - (contSize / 2), Stack())
        .pad(5.vmin)
    )
}
