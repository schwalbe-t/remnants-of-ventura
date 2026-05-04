
package schwalbe.ventura.client.game

import schwalbe.ventura.client.Client
import schwalbe.ventura.net.DownChatMessagePacket
import schwalbe.ventura.net.PacketHandler
import schwalbe.ventura.net.PacketType

class ChatMessageBuffer {

    companion object {
        const val MAX_COUNT: Int = 128
    }

    private val buffer: MutableList<DownChatMessagePacket> = mutableListOf()
    val lastMessages: List<DownChatMessagePacket> = this.buffer
    var receivedCount: Int = 0
        private set

    fun onMessageReceived(message: DownChatMessagePacket) {
        this.buffer.add(message)
        this.receivedCount += 1
        this.deleteOldMessages()
    }

    private fun deleteOldMessages() {
        val count: Int = this.buffer.size
        if (count <= MAX_COUNT) { return }
        val deleteCount: Int = count - MAX_COUNT
        this.buffer.subList(0, deleteCount).clear()
    }

    fun clear() {
        this.buffer.clear()
        this.receivedCount = 0
    }

}

fun PacketHandler<Unit>.addChatMessageHandling(client: Client) = this
    .onPacket(PacketType.DOWN_CHAT_MESSAGE) { msg, _ ->
        client.messages.onMessageReceived(msg)
    }
