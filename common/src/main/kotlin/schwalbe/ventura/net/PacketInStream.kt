
package schwalbe.ventura.net

import java.nio.ByteBuffer
import java.io.ByteArrayOutputStream
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.openReadChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.core.readBytes
import kotlinx.io.copyTo

class PacketInStream(inSocket: Socket, val maxPayloadSize: Int) {
    
    class InvalidPacketException(message: String) : Exception(message)

    companion object {
        private val packetTypes: Array<PacketType> = PacketType.values()
    }

    private val channel: ByteReadChannel = inSocket.openReadChannel()
    private val buffer = ByteArrayOutputStream()

    private fun tryReadBytes() {
        this.channel.readAvailable(8) { b ->
            var buff = b.readBytes()
            this.buffer.write(buff)
            buff.size
        }
    }

    fun tryRead(): Packet? {
        this.tryReadBytes()
        val buffered: ByteArray
        buffered = this.buffer.toByteArray()
        val view = ByteBuffer.wrap(buffered)
        val payloadOffset: Int = 6
        if (buffered.size < payloadOffset) { return null }
        val rawPacketType: Short = view.short
        val packetTypeValid: Boolean = rawPacketType >= 0
            && rawPacketType < PacketInStream.packetTypes.size
        if (!packetTypeValid) {
            throw InvalidPacketException("Invalid package type $rawPacketType")
        }
        val rawPayloadSize: Int = view.int
        val payloadSizeValid: Boolean = rawPayloadSize >= 0
            && rawPayloadSize < this.maxPayloadSize
        if (!payloadSizeValid) {
            throw InvalidPacketException("Invalid payload size $rawPayloadSize")
        }
        val totalSize: Int = payloadOffset + rawPayloadSize
        if (buffered.size < totalSize) { return null }
        val payload: ByteArray = buffered.copyOfRange(payloadOffset, totalSize)
        val packetType = PacketInStream.packetTypes[rawPacketType.toInt()]
        val remaining = if (totalSize < buffered.size) {
            buffered.copyOfRange(totalSize, buffered.size)
        } else {
            ByteArray(0)
        }
        this.buffer.reset()
        this.buffer.write(remaining)
        return Packet(packetType, payload)
    }

}