
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.Client
import schwalbe.ventura.client.LocalKeys
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.game.PersonAnim
import schwalbe.ventura.client.localized
import schwalbe.ventura.client.screens.GameScreen
import schwalbe.ventura.client.screens.Theme
import schwalbe.ventura.client.screens.googleSansSb
import schwalbe.ventura.client.screens.jetbrainsMonoSb
import schwalbe.ventura.data.Item
import schwalbe.ventura.data.ObjectInstance
import schwalbe.ventura.engine.input.Key
import schwalbe.ventura.engine.input.wasPressed
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.net.Packet
import schwalbe.ventura.net.PacketHandler
import schwalbe.ventura.net.PacketType
import schwalbe.ventura.net.TradeExecutionPacket

private enum class Mode(val key: LocalKeys) {
    PURCHASE(TITLE_PURCHASE), SELL(TITLE_SELL), NONE(BUTTON_GO_BACK)
}

private class ScreenState(val client: Client, val listName: String) {
    var mode: Mode = Mode.NONE
    var selectedItemDisplay: MsaaRenderDisplay? = null
    val selectedItemSection = Stack()
    val itemListSection = Stack()
}

private fun ScreenState.renderModeSwitcher(): UiElement {
    val buttonGap: UiSize = 1.vmin
    val buttonW: UiSize = (100.pw - buttonGap) / 2
    fun createModeButton(mode: Mode): UiElement = Stack()
        .add(if (this.mode == mode) {
            FlatBackground().withColor(Theme.BUTTON_HOVER_COLOR)
        } else {
            FlatBackground()
                .withColor(Theme.BUTTON_COLOR)
                .withHoverColor(Theme.BUTTON_HOVER_COLOR)
        })
        .add(Text()
            .withText(localized()[mode.key])
            .withSize(80.ph)
            .withFont(googleSansSb())
            .alignCenter()
            .pad(1.75.vmin)
        )
        .add(ClickArea().withLeftHandler {
            if (this.mode == mode) { return@withLeftHandler }
            this.switchMode(mode)
        })
        .wrapBorderRadius(0.75.vmin)
    return Axis.row()
        .add(buttonW, createModeButton(Mode.PURCHASE))
        .add(buttonGap, Space())
        .add(buttonW, createModeButton(Mode.SELL))
        .pad(buttonGap)
}

private fun ScreenState.switchMode(mode: Mode) {
    this.mode = mode
    this.renderModeSwitcher()
    when (mode) {
        Mode.PURCHASE -> client.network.outPackets?.send(Packet.serialize(
            PacketType.REQUEST_TRADES, this.listName
        ))
        Mode.SELL -> client.network.outPackets?.send(Packet.serialize(
            PacketType.REQUEST_PAWNS, Unit
        ))
        else -> {}
    }
}

private fun ScreenState.handleResponses(ph: PacketHandler<Unit>) {
    val l = localized()
    fun renderSelectedItemSection(
        item: Item?, count: Int, buttonStr: String, buttonHandler: () -> Unit
    ) {
        this.selectedItemSection.disposeAll()
        if (item == null || count <= 0) { return }
        val selectedItemDisplay = ItemDisplay.createDisplay(
            item,
            msaaSamples = 4,
            fixedAngle = null // null = rotates over time
        )
        this.selectedItemSection.add(createSelectedItemSection(
            item, count, selectedItemDisplay,
            itemActions = listOf(buttonStr to buttonHandler)
        ))
        this.selectedItemDisplay = selectedItemDisplay
    }
    ph.onPacket(PacketType.AVAILABLE_TRADES) { t, _ ->
        fun findTradeIdx(item: Item, count: Int): Int
            = t.trades.indexOfFirst { it.item == item && it.count == count }
        this.selectedItemSection.disposeAll()
        this.itemListSection.disposeAll().add(createItemListSection(
            title = this.renderModeSwitcher(),
            itemCounts = t.trades.associate { it.item to it.count },
            placeholder = null,
            onItemSelect = onItemSelect@{ item, count ->
                val tradeIdx: Int = findTradeIdx(item, count)
                if (tradeIdx == -1) { return@onItemSelect }
                val purchaseStr: String = l[BUTTON_PURCHASE]
                    .replace("{NAME}", l[item.type.localNameKey])
                    .replace("{COUNT}", count.toString())
                    .replace("{PRICE}", t.trades[tradeIdx].price.toString())
                renderSelectedItemSection(item, count, purchaseStr) {
                    this.client.network.outPackets?.send(Packet.serialize(
                        PacketType.EXECUTE_TRADE,
                        TradeExecutionPacket(this.listName, tradeIdx)
                    ))
                }
            }
        ))
    }
    ph.onPacket(PacketType.TRADE_COMPLETED) { _, _ ->
        this.client.toasts.show(TOAST_PURCHASE_SUCCESSFUL)
    }
    ph.onPacket(PacketType.AVAILABLE_PAWNS) { p, _ ->
        this.selectedItemSection.disposeAll()
        this.itemListSection.disposeAll().add(createItemListSection(
            title = this.renderModeSwitcher(),
            itemCounts = p.keys.associate { Item(it) to 1 },
            placeholder = null,
            onItemSelect = onItemSelect@{ item, count ->
                val reward: Int = p[item.type] ?: return@onItemSelect
                val sellString: String = l[BUTTON_SELL]
                    .replace("{NAME}", l[item.type.localNameKey])
                    .replace("{REWARD}", reward.toString())
                renderSelectedItemSection(item, count, sellString) {
                    this.client.network.outPackets?.send(Packet.serialize(
                        PacketType.EXECUTE_PAWN, item
                    ))
                }
            }
        ))
    }
    ph.onPacket(PacketType.PAWN_COMPLETED) { _, _ ->
        this.client.toasts.show(TOAST_SALE_SUCCESSFUL)
    }
}

fun shopScreen(
    client: Client, character: ObjectInstance, listName: String
): () -> GameScreen = {
    val state = ScreenState(client, listName)
    val toasts = ToastDisplay(client.toasts)
    val moneyBackground = BlurBackground().withRadius(2).withSpread(4)
    val moneyDisplay = Text()
    val screen = PausedScreen(
        client,
        camMode = { _ -> CameraModes.characterInRightThird(character) },
        closeIf = { Key.ESCAPE.wasPressed },
        playerFollowCursor = false,
        playerAnim = PersonAnim.thinking,
        render = {
            state.selectedItemDisplay?.invalidate()
            toasts.update()
            val numCoins = client.world
                ?.state?.lastReceived?.numCoins?.toString() ?: ""
            moneyDisplay.withText("${numCoins.padStart(12, '·')}¢")
            moneyBackground.invalidate()
        }
    )
    screen.screen.packets?.displayTaggedErrorToasts(toasts, client)
    state.handleResponses(screen.packets)
    state.switchMode(Mode.PURCHASE)
    val moneyDispW: UiSize = 15.vmin
    val moneyDispP: UiSize = 3.vmin
    screen.screen.add(layer = 0, element = Axis.row()
        .add(fpw * (2f/3f), Stack()
            .add(screen.background)
            .add(FlatBackground().withColor(Theme.PANEL_BACKGROUND))
            .add(Axis.row()
                .add(50.pw, state.itemListSection)
                .add(50.pw, state.selectedItemSection)
            )
        )
        .add(fpw * (1f/3f), Stack()
            .add(moneyBackground)
            .add(FlatBackground().withColor(Theme.PANEL_BACKGROUND))
            .add(moneyDisplay
                .withFont(jetbrainsMonoSb())
                .withSize(80.ph)
                .alignCenter()
                .pad(1.vmin)
            )
            .wrapBorderRadius(0.75.vmin)
            .withSize(moneyDispW, 4.vmin)
            .pad(
                left = 100.pw - moneyDispW - moneyDispP,
                right = moneyDispP,
                top = moneyDispP
            )
        )
    )
    screen.screen.add(layer = 1, element = toasts.root)
    screen.screen
}
