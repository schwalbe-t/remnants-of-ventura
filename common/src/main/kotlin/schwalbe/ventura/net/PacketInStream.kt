
package schwalbe.ventura.net

import java.nio.ByteBuffer
import java.io.ByteArrayOutputStream
import io.ktor.websocket.Frame

class PacketInStream(val maxPayloadSize: Int) {
    
    class InvalidPacketException(message: String) : Exception(message)


    private val buffer = ByteArrayOutputStream()

    @Synchronized
    fun handleBinaryFrame(frame: Frame) {
        when (frame) {
            is Frame.Binary -> {
                this.buffer.write(frame.data)
            }
            else -> {}
        }
    }

    @Synchronized
    fun tryRead(): Packet? {
        val buffered: ByteArray = this.buffer.toByteArray()
        val view = ByteBuffer.wrap(buffered)
        val payloadOffset: Int = 6
        if (buffered.size < payloadOffset) { return null }
        val packetType: Short = view.short
        val packetTypeValid: Boolean = packetType >= 0
            && packetType < PacketType.NUM_PACKET_TYPES
        if (!packetTypeValid) {
            throw InvalidPacketException("Invalid package type $packetType")
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