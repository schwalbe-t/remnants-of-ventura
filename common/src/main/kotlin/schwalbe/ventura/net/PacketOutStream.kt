
package schwalbe.ventura.net

import java.net.Socket
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.serializer
import java.nio.ByteBuffer

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
class PacketOutStream(val socket: Socket) {
    
    inline fun<reified T> send(packetType: PacketType, payload: T) {
        val binaryPayload = Cbor.encodeToByteArray(serializer<T>(), payload)
        this.sendRaw(packetType, binaryPayload)
    }

    fun sendRaw(packetType: PacketType, payload: ByteArray) {
        val buffer = ByteBuffer.allocate(2 + 4 + payload.size)
        buffer.putShort(packetType.ordinal.toShort())
        buffer.putInt(payload.size)
        buffer.put(payload)
        socket.outputStream.write(buffer.array())
        socket.outputStream.flush()
    }

}