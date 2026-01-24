
package schwalbe.ventura.net

import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.serializer
import kotlinx.serialization.DeserializationStrategy

class PacketHandler<C> {

    val handlers: MutableMap<PacketType, (ByteArray, C) -> Unit>
        = mutableMapOf()
    var onDecodeError: (C, String) -> Unit = { _, _ -> }

    inline fun<reified T> onPacket(
        packetType: PacketType, crossinline handler: (T, C) -> Unit
    ): PacketHandler<C> {
        val err: (C, String) -> Unit = this.onDecodeError
        this.handlers[packetType] = handler@{ payload: ByteArray, context: C ->
            val value: T
            try {
                value = Cbor.decodeFromByteArray<T>(
                    serializer<T>(), payload
                )
            } catch (e: Exception) {
                err(context, e.message ?: "")
                return@handler
            }
            try {
                handler(value, context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return this
    }

    fun handlePacket(packet: Packet, context: C) {
        val handler = this.handlers[packet.type]
        if (handler == null) { return }
        handler(packet.payload, context)
    }

    fun handleAll(packets: PacketInStream, context: C) {
        try {
            while (true) {
                val p: Packet? = packets.tryRead()
                if (p == null) { break }
                this.handlePacket(p, context)
            }
        } catch (e: Exception) {
            this.onDecodeError(context, e.message ?: "")
        }
    }

}
