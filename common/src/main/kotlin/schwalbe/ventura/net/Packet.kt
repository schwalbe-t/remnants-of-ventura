
package schwalbe.ventura.net

import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.serializer

/**
 * Represents a serialized packet in the Ventura protocol.
 * Each packet contains a packet [type] and a binary CBOR-serialized packet
 * [payload] (payload type depending on pocket type).
 * `PacketInStream` may be used to receive packets from a socket.
 * `PacketHandler` may be used to deserialize and handle different packet types.
 * `PacketOutStream` may be used to send sockets into a socket.
 */
class Packet(val type: PacketType, val payload: ByteArray) {
    
    companion object {
        
        /**
         * Creates a new packet from the given packet [type] and the given
         * unserialized [payload].
         * @param T the type of the unserialized payload
         * @return the created packet
         */
        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
        inline fun<reified T> serialize(type: PacketType, payload: T): Packet {
            val bytes = Cbor.encodeToByteArray(serializer<T>(), payload)
            return Packet(type, bytes)
        }
    
    }

}