
package schwalbe.ventura.net

import java.nio.ByteBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.Frame

class PacketOutStream(val socket: WebSocketSession, val scope: CoroutineScope) {

    fun send(packet: Packet) {
        val buffer = ByteBuffer.allocate(2 + 4 + packet.payload.size)
        buffer.putShort(packet.type)
        buffer.putInt(packet.payload.size)
        buffer.put(packet.payload)
        buffer.flip()
        val socket: WebSocketSession = this.socket
        this.scope.launch {
            try {
                socket.send(Frame.Binary(true, buffer.array()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}