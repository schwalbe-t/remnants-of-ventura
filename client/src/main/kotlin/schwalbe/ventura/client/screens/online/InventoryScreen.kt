
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.data.Item
import schwalbe.ventura.data.ItemType
import schwalbe.ventura.data.ItemVariant
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.net.PacketHandler

fun addInventoryItem(items: Axis, item: Item, count: Int) {
    val l = localized()
    val itemTitle = mutableListOf<Span>()
    itemTitle.add(Span(
        text = l[item.type.localNameKey],
        font = googleSansSb()
    ))
    val variant: ItemVariant? = item.variant
    if (variant != null) {
        itemTitle.add(Span(
            text = " (" + l[variant.localNameKey] + ")",
            font = googleSansR()
        ))
    }
    if (count > 1) {
        itemTitle.add(Span(
            text = " ×$count", font = googleSansR(),
            color = SECONDARY_BRIGHT_FONT_COLOR
        ))
    }
    items.add(8.vmin, Stack()
        .add(FlatBackground()
            .withColor(BUTTON_COLOR)
            .withHoverColor(BUTTON_HOVER_COLOR)
        )
        .add(Axis.row()
            .add(100.pw - 1.vmin - 100.ph, Axis.column()
                .add(60.ph, Text()
                    .withText(itemTitle)
                    .withColor(BRIGHT_FONT_COLOR)
                    .withSize(75.ph)
                )
                .add(40.ph, Text()
                    .withText(l[item.type.category.localNameKey])
                    .withColor(SECONDARY_BRIGHT_FONT_COLOR)
                    .withSize(75.ph)
                )
                .pad(top = 1.vmin, bottom = 1.vmin)
            )
            .add(1.vmin, Space())
            .add(100.ph, Space()) // TODO! inline item model render
            .pad(1.vmin)
        )
        .wrapBorderRadius(0.75.vmin)
        .pad(left = 1.vmin, right = 1.vmin)
    )
    items.add(1.vmin, Space())
}

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
    for (i in 0..10) {
        addInventoryItem(
            items,
            Item(ItemType.TEST, ItemVariant.TEST_RED_HOODIE),
            count = 1
        )
    }
    for (i in 0..10) {
        addInventoryItem(
            items,
            Item(ItemType.TEST),
            count = 123
        )
    }
    items.add(50.ph, Space())
    screen.add(layer = 0, element = Axis.row()
        .add(fpw * (2f/3f), Stack()
            .add(background)
            .add(Axis.row()
                .add(50.pw, Axis.column()
                    .add(8.vmin, Text()
                        .withText("Inventory")
                        .withColor(BRIGHT_FONT_COLOR)
                        .withFont(googleSansSb())
                        .withSize(85.ph)
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
                            .withColor(BRIGHT_FONT_COLOR)
                            .withFont(googleSansSb())
                            .withSize(75.ph)
                        )
                        .add(40.ph, Text()
                            .withText("Selected Item Type")
                            .withColor(SECONDARY_BRIGHT_FONT_COLOR)
                            .withSize(75.ph)
                        )
                        .pad(1.5.vmin)
                    )
                    .add(100.ph - 8.vmin - 5.vmin - 20.ph, Space()) // TODO! inline item model render
                    .add(5.vmin, Axis.row()
                        .add(1.vmin, Space())
                        .add(50.pw - 1.vmin - 0.5.vmin, createTextButton(
                            content = "Action 1",
                            textColor = BRIGHT_FONT_COLOR,
                            handler = {}
                        ))
                        .add(1.vmin, Space())
                        .add(50.pw - 1.vmin - 0.5.vmin, createTextButton(
                            content = "Action 2",
                            textColor = BRIGHT_FONT_COLOR,
                            handler = {}
                        ))
                        .add(1.vmin, Space())
                        .pad(top = 1.vmin)
                    )
                    .add(20.ph, Text()
                        .withText("Selected Item Description\n" + "Lorem ipsum dalor test test ".repeat(100))
                        .withColor(BRIGHT_FONT_COLOR)
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
