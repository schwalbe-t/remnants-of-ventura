
package schwalbe.ventura.net

import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.serializer
import kotlinx.serialization.DeserializationStrategy

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
class PacketHandler<C> {

    val handlers: MutableMap<PacketType, (ByteArray, C) -> Unit>
        = mutableMapOf()

    inline fun<reified T> onPacket(
        packetType: PacketType, crossinline handler: (T, C) -> Unit
    ) {
        this.handlers[packetType] = { payload: ByteArray, context: C ->
            val value: T = Cbor.decodeFromByteArray<T>(serializer<T>(), payload)
            handler(value, context)
        }
    }

    fun handlePacket(packet: Packet, context: C) {
        val handler = this.handlers[packet.type]
        if (handler == null) { return }
        handler(packet.payload, context)
    }

    fun handleAll(packets: PacketInStream, context: C) {
        while (true) {
            val p: Packet? = packets.tryRead()
            if (p == null) { break }
            this.handlePacket(p, context)
        }
    }

}
