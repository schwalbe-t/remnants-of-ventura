
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.data.*
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.net.*

fun createInventoryItemTitle(item: Item, count: Int): List<Span> {
    val l = localized()
    val itemTitle = mutableListOf<Span>()
    itemTitle.add(Span(
        text = l[item.type.localNameKey],
        font = googleSansSb(),
        color = BRIGHT_FONT_COLOR
    ))
    val variant: ItemVariant? = item.variant
    if (variant != null) {
        itemTitle.add(Span(
            text = " (" + l[variant.localNameKey] + ")",
            font = googleSansR(),
            color = BRIGHT_FONT_COLOR
        ))
    }
    if (count > 1) {
        itemTitle.add(Span(
            text = " ×$count",
            font = googleSansR(),
            color = SECONDARY_BRIGHT_FONT_COLOR
        ))
    }
    return itemTitle
}

fun addInventoryItem(
    items: Axis, item: Item, count: Int, onClick: (Item, Int) -> Unit
) {
    items.add(8.vmin, Stack()
        .add(FlatBackground()
            .withColor(BUTTON_COLOR)
            .withHoverColor(BUTTON_HOVER_COLOR)
        )
        .add(Axis.row()
            .add(100.pw - 1.vmin - 100.ph, Axis.column()
                .add(60.ph, Text()
                    .withText(createInventoryItemTitle(item, count))
                    .withColor(BRIGHT_FONT_COLOR)
                    .withSize(75.ph)
                )
                .add(40.ph, Text()
                    .withText(localized()[item.type.category.localNameKey])
                    .withColor(SECONDARY_BRIGHT_FONT_COLOR)
                    .withSize(75.ph)
                )
                .pad(top = 1.vmin, bottom = 1.vmin)
            )
            .add(1.vmin, Space())
            .add(100.ph, ItemDisplay.createDisplay(
                item,
                fixedAngle = 0f,
                msaaSamples = 4
            ))
            .pad(1.vmin)
        )
        .add(ClickArea().withHandler { onClick(item, count) })
        .wrapBorderRadius(0.75.vmin)
        .pad(left = 1.vmin, right = 1.vmin)
    )
    items.add(1.vmin, Space())
}

private fun createItemListSection(
    client: Client, packetHandler: PacketHandler<Unit>,
    onItemSelect: (Item, Int) -> Unit
): Axis {
    val l = localized()
    val itemList = Axis.column()
    packetHandler.onPacket(PacketType.INVENTORY_CONTENTS) { inventory, _ ->
        val itemCounts = inventory.itemCounts
            .asSequence().filter { (_, c) -> c > 0 }.toList()
            .sortedBy { (i, _) -> l[i.type.localNameKey] }
        for ((item, count) in itemCounts) {
            addInventoryItem(itemList, item, count, onItemSelect)
        }
        if (itemCounts.isEmpty()) {
            itemList.add(2.vmin, Text()
                .withText(l[PLACEHOLDER_INVENTORY_EMPTY])
                .withSize(85.ph)
                .withFont(googleSansI())
                .withColor(BRIGHT_FONT_COLOR)
                .pad(left = 2.5.vmin, right = 2.5.vmin)
            )
        }
        itemList.add(50.ph, Space())
    }
    client.network.outPackets?.send(Packet.serialize(
        PacketType.REQUEST_INVENTORY_CONTENTS, Unit
    ))
    return Axis.column()
        .add(8.vmin, Text()
            .withText(l[TITLE_INVENTORY])
            .withColor(BRIGHT_FONT_COLOR)
            .withFont(googleSansSb())
            .withSize(85.ph)
            .pad(2.5.vmin)
        )
        .add(100.ph - 8.vmin, itemList
            .wrapScrolling()
            .withThumbColor(BUTTON_COLOR)
            .withThumbHoverColor(BUTTON_HOVER_COLOR)
        )
}

private fun createInventoryItemActionButton(
    content: String, handler: () -> Unit
): UiElement = Stack()
    .add(FlatBackground()
        .withColor(BUTTON_COLOR)
        .withHoverColor(BUTTON_HOVER_COLOR)
    )
    .add(Text()
        .withText(content)
        .withColor(BRIGHT_FONT_COLOR)
        .withSize(70.ph)
        .alignCenter()
        .pad(0.75.vmin)
    )
    .add(ClickArea().withHandler(handler))
    .wrapBorderRadius(0.75.vmin)

private fun createSelectedItemSection(
    item: Item, count: Int, itemDisplay: MsaaRenderDisplay
): Axis {
    val l = localized()
    return Axis.column()
        .add(8.vmin, Axis.column()
            .add(60.ph, Text()
                .withText(createInventoryItemTitle(item, count))
                .withSize(75.ph)
            )
            .add(40.ph, Text()
                .withText(l[item.type.category.localNameKey])
                .withColor(SECONDARY_BRIGHT_FONT_COLOR)
                .withSize(75.ph)
            )
            .pad(1.5.vmin)
        )
        .add(100.ph - 8.vmin - 5.vmin - 20.ph, Axis.column()
            .add((100.ph - 100.pmin) / 2, Space())
            .add(100.pmin, itemDisplay)
            .add((100.ph - 100.pmin) / 2, Space())
            .pad(3.vmin)
        )
        .add(5.vmin, Axis.row()
            .add(1.vmin, Space())
            .add(50.pw - 1.vmin - 0.5.vmin, createInventoryItemActionButton(
                content = "Action 1",
                handler = {}
            ))
            .add(1.vmin, Space())
            .add(50.pw - 1.vmin - 0.5.vmin, createInventoryItemActionButton(
                content = "Action 2",
                handler = {}
            ))
            .add(1.vmin, Space())
            .pad(top = 1.vmin)
        )
        .add(20.ph, Text()
            .withText(l[item.type.localDescKey])
            .withColor(BRIGHT_FONT_COLOR)
            .wrapScrolling()
            .withThumbColor(BUTTON_COLOR)
            .withThumbHoverColor(BUTTON_HOVER_COLOR)
            .pad(1.5.vmin)
        )
}

fun inventoryMenuScreen(client: Client): () -> GameScreen = {
    client.world?.camController?.mode = CameraController.Mode.PLAYER_ON_SIDE
    val renderWorld = renderGameworld(client)
    val background = BlurBackground()
        .withRadius(3)
        .withSpread(5)
    var selectedItemDisplay: MsaaRenderDisplay? = null
    val packets = PacketHandler.receiveDownPackets<Unit>()
        .addErrorLogging()
        .addWorldHandling(client)
    val screen = GameScreen(
        render = {
            if (Key.ESCAPE.wasPressed || Key.TAB.wasPressed) {
                client.nav.pop()
            }
            renderWorld()
            background.invalidate()
            selectedItemDisplay?.invalidate()
        },
        networkState = keepNetworkConnectionAlive(client, onFail = { reason ->
            client.nav.replace(serverConnectionFailedScreen(reason, client))
            client.network.clearError()
        }),
        packets = packets,
        navigator = client.nav
    )
    val selectedItemSection = Stack()
    screen.add(layer = 0, element = Axis.row()
        .add(fpw * (2f/3f), Stack()
            .add(background)
            .add(FlatBackground().withColor(PANEL_BACKGROUND))
            .add(Axis.row()
                .add(50.pw, createItemListSection(
                    client, packets,
                    onItemSelect = { item, count ->
                        selectedItemDisplay = ItemDisplay.createDisplay(
                            item,
                            msaaSamples = 4,
                            fixedAngle = null // null = rotates over time
                        )
                        selectedItemSection.withoutContent()
                        selectedItemSection.add(createSelectedItemSection(
                            item, count, selectedItemDisplay
                        ))
                    }
                ))
                .add(50.pw, selectedItemSection)
            )
        )
        .add(fpw * (1f/3f), Space())
    )
    screen
}
