
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.data.*
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.net.*
import kotlin.math.PI

fun createInventoryItemTitle(item: Item, count: Int): List<Span> {
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
            text = " ×$count",
            font = googleSansR(),
            color = Theme.SECONDARY_FONT_COLOR
        ))
    }
    return itemTitle
}

fun addInventoryItem(
    items: Axis, item: Item?, count: Int, onClick: () -> Unit
) {
    val root = Stack()
    root.add(FlatBackground()
        .withColor(Theme.BUTTON_COLOR)
        .withHoverColor(Theme.BUTTON_HOVER_COLOR)
    )
    if (item != null) {
        root.add(Axis.row()
            .add(100.pw - 1.vmin - 100.ph, Axis.column()
                .add(60.ph, Text()
                    .withText(createInventoryItemTitle(item, count))
                    .withSize(75.ph)
                )
                .add(40.ph, Text()
                    .withText(localized()[item.type.category.localNameKey])
                    .withColor(Theme.SECONDARY_FONT_COLOR)
                    .withSize(75.ph)
                )
                .pad(top = 1.vmin, bottom = 1.vmin)
            )
            .add(1.vmin, Space())
            .add(100.ph, ItemDisplay.createDisplay(
                item,
                fixedAngle = -(PI.toFloat() / 4f), // 180/4 = 45 degrees
                msaaSamples = 4
            ))
            .pad(1.vmin)
        )
    }
    root.add(ClickArea().withLeftHandler(onClick))
    items.add(8.vmin, root
        .wrapBorderRadius(0.75.vmin)
        .pad(left = 1.vmin, right = 1.vmin)
    )
    items.add(1.vmin, Space())
}

fun createInventoryTitle(): UiElement = Text()
    .withText(localized()[TITLE_INVENTORY])
    .withFont(googleSansSb())
    .withSize(75.ph)
    .pad(2.5.vmin)

fun createItemListSection(
    title: UiElement = createInventoryTitle(),
    itemCounts: Map<Item, Int>,
    placeholder: LocalKeys? = PLACEHOLDER_INVENTORY_EMPTY,
    displayedEntries: (Item, Int) -> Boolean = { _, _ -> true },
    onItemSelect: (Item, Int) -> Unit
): UiElement {
    val l = localized()
    val itemList = Axis.column()
    val itemCounts = itemCounts.asSequence()
        .filter { (_, c) -> c > 0 }
        .filter { (i, c) -> displayedEntries(i, c) }
        .sortedBy { (i, _) -> l[i.type.localNameKey] }
    var isEmpty = true
    for ((item, count) in itemCounts) {
        isEmpty = false
        addInventoryItem(itemList, item, count) {
            onItemSelect(item, count)
        }
    }
    if (isEmpty && placeholder != null) {
        itemList.add(10.vmin, Text()
            .withText(l[placeholder])
            .withSize(1.5.vmin)
            .withFont(googleSansI())
            .pad(left = 2.5.vmin, right = 2.5.vmin)
        )
    }
    itemList.add(50.ph, Space())
    return Axis.column()
        .add(8.vmin, title)
        .add(100.ph - 8.vmin, itemList.wrapThemedScrolling(horiz = false))
        .pad(bottom = 1.vmin)
}

fun requestInventoryContents(client: Client) {
    client.network.outPackets?.send(Packet.serialize(
        PacketType.REQUEST_INVENTORY_CONTENTS, Unit
    ))
}

fun createInventoryItemActionButton(
    content: String, handler: () -> Unit
): UiElement = Stack()
    .add(FlatBackground()
        .withColor(Theme.BUTTON_COLOR)
        .withHoverColor(Theme.BUTTON_HOVER_COLOR)
    )
    .add(Text()
        .withText(content)
        .withSize(70.ph)
        .alignCenter()
        .pad(0.75.vmin)
    )
    .add(ClickArea().withLeftHandler(handler))
    .wrapBorderRadius(0.75.vmin)

fun createSelectedItemSection(
    item: Item, count: Int, itemDisplay: MsaaRenderDisplay,
    itemActions: List<Pair<String, () -> Unit>>
): Axis {
    val l = localized()
    val actionButtons = Axis.row()
    val paddingSum: UiSize = (itemActions.size + 1) * 1.vmin
    val buttonSize: UiSize = (100.pw - paddingSum) / itemActions.size
    for ((actionName, actionHandler) in itemActions) {
        actionButtons.add(1.vmin, Space())
        actionButtons.add(buttonSize, createInventoryItemActionButton(
            content = actionName, handler = actionHandler
        ))
    }
    actionButtons.add(1.vmin, Space())
    return Axis.column()
        .add(8.vmin, Axis.column()
            .add(60.ph, Text()
                .withText(createInventoryItemTitle(item, count))
                .withSize(75.ph)
            )
            .add(40.ph, Text()
                .withText(l[item.type.category.localNameKey])
                .withColor(Theme.SECONDARY_FONT_COLOR)
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
        .add(5.vmin, actionButtons
            .pad(top = 1.vmin)
        )
        .add(20.ph, Text()
            .withText(l[item.type.localDescKey])
            .withSize(1.5.vmin)
            .wrapThemedScrolling(horiz = false)
            .pad(1.5.vmin)
        )
}

fun inventoryMenuScreen(client: Client): () -> GameScreen = {
    var selectedItemDisplay: MsaaRenderDisplay? = null
    val toasts = ToastDisplay(client.toasts)
    val screen = PausedScreen(
        client,
        camMode = { w -> CameraModes.playerInRightThird(w.player) },
        closeIf = { Key.ESCAPE.wasPressed || Key.TAB.wasPressed },
        playerAnim = PersonAnim.thinking,
        render = {
            selectedItemDisplay?.invalidate()
            toasts.update()
        }
    )
    screen.screen.packets?.displayTaggedErrorToasts(toasts, client)
    val selectedItemSection = Stack()
    fun renderSelectedItemSection(
        item: Item?, count: Int, afterAction: () -> Unit
    ) {
        selectedItemSection.disposeAll()
        if (item == null || count <= 0) { return }
        selectedItemDisplay = ItemDisplay.createDisplay(
            item,
            msaaSamples = 4,
            fixedAngle = null // null = rotates over time
        )
        val itemActions = item.type.category.actions.map {
            localized()[it.localNameKey] to {
                ITEM_ACTION_HANDLERS[it]?.invoke(item, client)
                afterAction()
            }
        }
        selectedItemSection.add(createSelectedItemSection(
            item, count, selectedItemDisplay, itemActions
        ))
    }
    var selectedItem: Item? = null
    fun afterItemAction(): Unit = requestInventoryContents(client)
    val itemListSection = Stack()
    screen.screen.packets?.onPacket(PacketType.INVENTORY_CONTENTS) { i, _ ->
        itemListSection.disposeAll()
        itemListSection.add(createItemListSection(
            itemCounts = i.itemCounts,
            onItemSelect = { item, count ->
                selectedItem = item
                renderSelectedItemSection(
                    item, count, ::afterItemAction
                )
            },
        ))
        renderSelectedItemSection(
            selectedItem, i.itemCounts[selectedItem] ?: 0, ::afterItemAction
        )
    }
    requestInventoryContents(client)
    screen.screen.add(layer = 0, element = Axis.row()
        .add(fpw * (2f/3f), Stack()
            .add(screen.background)
            .add(FlatBackground().withColor(Theme.PANEL_BACKGROUND))
            .add(Axis.row()
                .add(50.pw, itemListSection)
                .add(50.pw, selectedItemSection)
            )
        )
        .add(fpw * (1f/3f), Space())
    )
    screen.screen.add(layer = 1, element = toasts.root)
    screen.screen
}
