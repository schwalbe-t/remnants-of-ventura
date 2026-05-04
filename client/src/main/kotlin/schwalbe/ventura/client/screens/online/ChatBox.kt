
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.MAX_CHAT_MESSAGE_LENGTH
import schwalbe.ventura.client.Client
import schwalbe.ventura.client.localized
import schwalbe.ventura.client.LocalKeys.PLACEHOLDER_WRITE_MESSAGE
import schwalbe.ventura.client.screens.Theme
import schwalbe.ventura.client.screens.wrapThemedScrolling
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.net.Packet
import schwalbe.ventura.net.PacketHandler
import schwalbe.ventura.net.PacketType

class ChatBox(val client: Client) {

    val background = BlurBackground().withRadius(2).withSpread(4)
    val lines = Axis.column()
    val root = Stack()
        .add(this.background)
        .add(FlatBackground().withColor(Theme.PANEL_BACKGROUND))
        .add(Axis.column()
            .add(100.ph - 4.vmin, this.lines
                .wrapThemedScrolling(horiz = false, vert = true)
            )
            .add(4.vmin, Stack()
                .add(FlatBackground().withColor(Theme.BUTTON_COLOR))
                .add(TextInput()
                    .withContent(Text().withSize(70.ph))
                    .withPlaceholder(Text()
                        .withText(localized()[PLACEHOLDER_WRITE_MESSAGE])
                        .withSize(70.ph)
                        .withColor(Theme.SECONDARY_FONT_COLOR)
                    )
                    .let { input -> input.withTypedText {
                        if (it == "\n") {
                            this.onMessageSend(input.valueString)
                            input.withValue("")
                        } else if (input.value.size < MAX_CHAT_MESSAGE_LENGTH) {
                            input.writeText(it)
                        }
                    } }
                    .withMultilineInput(true)
                    .wrapScrolling()
                    .withBarsEnabled(horiz = true, vert = false)
                    .pad(1.2.vmin)
                )
            )
        )
        .wrapBorderRadius(0.75.vmin)

    private fun onMessageSend(msg: String) {
        this.client.network.outPackets?.send(Packet.serialize(
            PacketType.UP_CHAT_MESSAGE, msg
        ))
    }

    fun handleChatMessages(packets: PacketHandler<Unit>) {
        packets.onPacketUntil(
            PacketType.DOWN_CHAT_MESSAGE, until = this.root::wasDisposed
        ) { msg, _ ->
            // TODO!
        }
    }

    fun update() {
        this.background.invalidate()
    }

}
