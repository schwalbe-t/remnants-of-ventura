
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.MAX_CHAT_MESSAGE_LENGTH
import schwalbe.ventura.CHAT_MESSAGE_COOLDOWN_MS
import schwalbe.ventura.client.Client
import schwalbe.ventura.client.localized
import schwalbe.ventura.client.LocalKeys.PLACEHOLDER_WRITE_MESSAGE
import schwalbe.ventura.client.screens.Theme
import schwalbe.ventura.client.screens.googleSansSb
import schwalbe.ventura.client.screens.wrapThemedScrolling
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.net.Packet
import schwalbe.ventura.net.PacketHandler
import schwalbe.ventura.net.PacketType
import org.joml.Vector4f
import org.joml.Vector4fc

class ChatBox(val client: Client) {

    companion object {
        val UNFOCUS_COLOR: Vector4fc = Vector4f(0.2f, 0.2f, 0.2f, 0.05f)
        const val SEND_COOLDOWN_MS: Long = CHAT_MESSAGE_COOLDOWN_MS + 100
    }

    val blurBackground = BlurBackground().withRadius(2).withSpread(4)
    val baseBackground = FlatBackground()

    val lines = Text()
    val scrolledLines = this.lines
        .withSize(1.5.vmin)
        .wrapThemedScrolling(horiz = false, vert = true)
        .withStickToBottom()

    val inputBackground = FlatBackground()
    val inputText = Text().withSize(1.3.vmin)
    val input: TextInput = TextInput()
        .withContent(this.inputText)
        .withPlaceholder(Text()
            .withText(localized()[PLACEHOLDER_WRITE_MESSAGE])
            .withSize(1.3.vmin)
            .withColor(Theme.SECONDARY_FONT_COLOR)
        )
        .withTypedText {
            if (it == "\n") {
                if (this.isOnSendCooldown()) { return@withTypedText }
                val message = this.input.valueString.trim()
                if (message.isNotEmpty()) {
                    this.onMessageSend(message)
                }
                this.input.withValue("")
                this.client.nav.currentOrNull?.let { screen ->
                    this.input.gainFocus(screen)
                }
            } else if (input.value.size < MAX_CHAT_MESSAGE_LENGTH) {
                this.input.writeText(it)
            }
        }
        .withMultilineInput(true)

    val hoverDetector = ClickArea()

    val root = Stack()
        .add(this.blurBackground)
        .add(this.baseBackground)
        .add(Axis.column()
            .add(100.ph - 4.vmin, this.scrolledLines
                .pad(1.vmin)
            )
            .add(4.vmin, Stack()
                .add(this.inputBackground)
                .add(this.input
                    .wrapScrolling()
                    .withBarsEnabled(horiz = true, vert = false)
                    .pad(1.1.vmin)
                )
            )
        )
        .add(this.hoverDetector)
        .wrapBorderRadius(0.75.vmin)

    private var lastSendTime: Long = 0L

    private fun isOnSendCooldown(): Boolean {
        val now: Long = System.currentTimeMillis()
        return now - this.lastSendTime < SEND_COOLDOWN_MS
    }

    private fun onMessageSend(msg: String) {
        this.lastSendTime = System.currentTimeMillis()
        this.client.network.outPackets?.send(Packet.serialize(
            PacketType.UP_CHAT_MESSAGE, msg
        ))
    }

    fun handleChatMessages(packets: PacketHandler<Unit>) {
        packets.onPacketUntil(
            PacketType.DOWN_CHAT_MESSAGE, until = this.root::wasDisposed
        ) { _, _ ->
            this.renderMessageLog()
        }
    }

    fun renderMessageLog() {
        this.lines.withText(this.client.messages.lastMessages.flatMap { msg ->
            val sender: String = msg.senderName?.let { "$it:  " } ?: ""
            listOf(
                Span(text = sender, font = googleSansSb()),
                Span(text = msg.message + "\n")
            )
        })
    }

    var isTyping: Boolean = false
        private set
    var inFocus: Boolean = false
        private set

    fun update() {
        val screen = this.client.nav.currentOrNull ?: return
        this.isTyping = screen.currentlyInFocus == this.input
        this.inFocus = this.hoverDetector.isHovering || this.isTyping
        this.blurBackground.invalidate()
        this.baseBackground.withColor(
            if (this.inFocus) Theme.PANEL_BACKGROUND else UNFOCUS_COLOR
        )
        this.inputBackground.withColor(
            if (this.inFocus) Theme.BUTTON_COLOR else UNFOCUS_COLOR
        )
        this.inputText.withColor(
            if (this.isOnSendCooldown()) Theme.SECONDARY_FONT_COLOR else null
        )
    }

}
