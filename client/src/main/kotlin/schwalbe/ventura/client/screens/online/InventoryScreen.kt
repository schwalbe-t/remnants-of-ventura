
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.net.PacketHandler

fun inventoryMenuScreen(client: Client): () -> GameScreen = {
    client.world?.camController?.mode = CameraController.Mode.PLAYER_ON_SIDE
    val renderWorld = renderGameworld(client)
    val background = BlurBackground()
        .withRadius(3)
        .withSpread(5)
    val screen = GameScreen(
        render = {
            if (Key.ESCAPE.wasPressed || Key.TAB.wasPressed) {
                client.nav.pop()
            }
            renderWorld()
            background.invalidate()
        },
        networkState = keepNetworkConnectionAlive(client, onFail = { reason ->
            client.nav.replace(serverConnectionFailedScreen(reason, client))
            client.network.clearError()
        }),
        packets = PacketHandler<Unit>()
            .addErrorLogging()
            .addWorldHandling(client),
        navigator = client.nav
    )
    val items = Axis.column()
    for (i in 0..20) {
        items.add(8.vmin, Stack()
            .add(FlatBackground()
                .withColor(BUTTON_COLOR)
                .withHoverColor(BUTTON_HOVER_COLOR)
            )
            .add(Axis.row()
                .add(100.pw - 1.vmin - 100.ph, Axis.column()
                    .add(60.ph, Text()
                        .withText("Item Name ×123")
                        .withFont(googleSansSb())
                        .withSize(75.ph)
                    )
                    .add(40.ph, Text()
                        .withText("Item Type")
                        .withFont(googleSansI())
                        .withSize(75.ph)
                    )
                    .pad(top = 1.vmin, bottom = 1.vmin)
                )
                .add(1.vmin, Space())
                .add(100.ph, FlatBackground()
                    .withColor(0, 0, 255, 50)
                )
                .pad(1.vmin)
            )
            .wrapBorderRadius(0.75.vmin)
            .pad(left = 1.vmin, right = 1.vmin)
        )
        items.add(1.vmin, Space())
    }
    items.add(50.ph, Space())
    screen.add(layer = 0, element = Axis.row()
        .add(fpw * (2f/3f), Stack()
            .add(background)
            .add(Axis.row()
                .add(50.pw, Axis.column()
                    .add(8.vmin, Text()
                        .withText("Inventory")
                        .withFont(googleSansSb())
                        .withSize(75.ph)
                        .pad(2.5.vmin)
                    )
                    .add(100.ph - 8.vmin, items
                        .wrapScrolling()
                        .withThumbColor(BUTTON_COLOR)
                        .withThumbHoverColor(BUTTON_HOVER_COLOR)
                    )
                )
                .add(50.pw, Axis.column()
                    .add(8.vmin, Axis.column()
                        .add(60.ph, Text()
                            .withText("Selected Item Name")
                            .withFont(googleSansSb())
                            .withSize(75.ph)
                        )
                        .add(40.ph, Text()
                            .withText("Selected Item Type")
                            .withFont(googleSansI())
                            .withSize(75.ph)
                        )
                        .pad(1.5.vmin)
                    )
                    .add(100.ph - 8.vmin - 5.vmin - 20.ph, FlatBackground()
                        .withColor(0, 0, 255, 50)
                    )
                    .add(5.vmin, Axis.row()
                        .add(1.vmin, Space())
                        .add(50.pw - 1.vmin - 0.5.vmin,
                            createTextButton("Action 1") {}
                        )
                        .add(1.vmin, Space())
                        .add(50.pw - 1.vmin - 0.5.vmin,
                            createTextButton("Action 2") {}
                        )
                        .add(1.vmin, Space())
                        .pad(top = 1.vmin)
                    )
                    .add(20.ph, Text()
                        .withText("Selected Item Description\n" + "Lorem ipsum dalor test test ".repeat(100))
                        .wrapScrolling()
                        .withThumbColor(BUTTON_COLOR)
                        .withThumbHoverColor(BUTTON_HOVER_COLOR)
                        .pad(1.5.vmin)
                    )
                )
            )
        )
        .add(fpw * (1f/3f), Space())
    )
    screen
}
