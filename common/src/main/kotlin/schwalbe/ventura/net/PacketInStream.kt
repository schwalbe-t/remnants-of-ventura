
package schwalbe.ventura.net

import java.net.Socket
import java.nio.ByteBuffer
import java.io.ByteArrayOutputStream
import java.io.InputStream

class PacketInStream(val socket: Socket, val maxPayloadSize: Int) {
    
    class InvalidPacketException(message: String) : Exception(message)

    companion object {
        private val packetTypes: Array<PacketType> = PacketType.values()
    }

    val packets: ArrayDeque<Packet> = ArrayDeque()

    private val buffer = ByteArrayOutputStream()
    private val chunk = ByteArray(1024)

    private fun readBytes() {
        val input: InputStream = this.socket.inputStream
        while (true) {
            val remNumBytes: Int = input.available()
            if (remNumBytes <= 0) { break }
            val tryReadNum: Int = minOf(remNumBytes, this.chunk.size)
            val readNum: Int = input.read(this.chunk, 0, tryReadNum)
            if (readNum == -1) { break }
            this.buffer.write(this.chunk, 0, readNum)
        }
    }

    private fun readPacket(): Boolean {
        val buffered: ByteArray = this.buffer.toByteArray()
        val view = ByteBuffer.wrap(buffered)
        val payloadOffset: Int = 6
        if (buffered.size < payloadOffset) { return false }
        val rawPacketType: Short = view.getShort(0)
        val packetTypeValid: Boolean = rawPacketType >= 0
            && rawPacketType < PacketInStream.packetTypes.size
        if (!packetTypeValid) {
            throw InvalidPacketException("Invalid package type $rawPacketType")
        }
        val rawPayloadSize: Int = view.getInt(2)
        val payloadSizeValid: Boolean = rawPayloadSize >= 0
            && rawPayloadSize < this.maxPayloadSize
        if (!payloadSizeValid) {
            throw InvalidPacketException("Invalid payload size $rawPayloadSize")
        }
        val totalSize: Int = payloadOffset + rawPayloadSize
        if (buffered.size < totalSize) { return false }
        val payload: ByteArray = buffered.copyOfRange(payloadOffset, totalSize)
        val packetType = PacketInStream.packetTypes[rawPacketType.toInt()]
        this.packets.add(Packet(packetType, payload))
        val remaining = buffered.copyOfRange(totalSize, buffered.size)
        this.buffer.reset()
        this.buffer.write(remaining)
        return true
    }

    fun readPackets() {
        while (this.readPacket()) {}
    }

}