
package schwalbe.ventura.net

import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.serializer
import kotlinx.serialization.DeserializationStrategy

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
class PacketHandler {

    val handlers: MutableMap<PacketType, (ByteArray) -> Unit> = mutableMapOf()

    inline fun<reified T> onPacket(
        packetType: PacketType, crossinline handler: (T) -> Unit
    ) {
        this.handlers[packetType] = { payload: ByteArray ->
            val value: T = Cbor.decodeFromByteArray<T>(serializer<T>(), payload)
            handler(value)
        }
    }

    fun handlePacket(packetType: PacketType, payload: ByteArray) {
        val handler = this.handlers[packetType]
        if (handler == null) { return }
        handler(payload)
    }

}
