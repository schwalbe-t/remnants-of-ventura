
package schwalbe.ventura.net

import java.nio.ByteBuffer
import java.io.ByteArrayOutputStream
import io.ktor.websocket.Frame

class PacketInStream(val maxPayloadSize: Int) {
    
    class InvalidPacketException(message: String) : Exception(message)

    companion object {
        private val packetTypes: Array<PacketType> = PacketType.values()
    }

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