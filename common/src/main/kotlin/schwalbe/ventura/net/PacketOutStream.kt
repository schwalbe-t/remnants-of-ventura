
package schwalbe.ventura.net

import java.nio.ByteBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeByteArray

class PacketOutStream(outSocket: Socket, val scope: CoroutineScope) {
    
    private val channel: ByteWriteChannel
        = outSocket.openWriteChannel(autoFlush = true)

    fun send(packet: Packet) {
        val buffer = ByteBuffer.allocate(2 + 4 + packet.payload.size)
        buffer.putShort(packet.type.ordinal.toShort())
        buffer.putInt(packet.payload.size)
        buffer.put(packet.payload)
        buffer.flip()
        val channel: ByteWriteChannel = this.channel
        this.scope.launch {
            try {
                channel.writeByteArray(buffer.array())
            } catch (e: Exception) {
                println("Failed to send message: ${e.message}")
            }
        }
    }

}