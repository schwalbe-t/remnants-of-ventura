
package schwalbe.ventura.net

import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.serializer

class PacketHandler<C>(val receivingDir: PacketDirection) {

    companion object {
        fun <C> receiveUpPackets() = PacketHandler<C>(PacketDirection.UP)
        fun <C> receiveDownPackets() = PacketHandler<C>(PacketDirection.DOWN)
    }


    val handlers: MutableMap<Short, (ByteArray, C) -> Unit>
        = mutableMapOf()
    var onDecodeError: (C, String) -> Unit = { _, _ -> }

    inline fun <reified P> onPacket(
        packetType: PacketType<P>, crossinline handler: (P, C) -> Unit
    ): PacketHandler<C> {
        require(packetType.direction == this.receivingDir) {
            "Packet handler was configured to receive packets of direction " +
            "'${this.receivingDir.name}', but a packet type with direction " +
            "'${packetType.direction.name}' was attempted to be registered"
        }
        val err: (C, String) -> Unit = this.onDecodeError
        val packetTypeId: Short = packetType.ordinal.toShort()
        this.handlers[packetTypeId] = handler@{ rpl: ByteArray, ctx: C ->
            val decPayload: P
            try {
                decPayload = Cbor.decodeFromByteArray(serializer<P>(), rpl)
            } catch (e: Exception) {
                err(ctx, e.message ?: "")
                return@handler
            }
            try {
                handler(decPayload, ctx)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return this
    }

    fun handlePacket(packet: Packet, context: C) {
        val handler = this.handlers[packet.type] ?: return
        handler(packet.payload, context)
    }

    fun handleAll(packets: PacketInStream, context: C) {
        try {
            while (true) {
                val p: Packet = packets.tryRead() ?: break
                this.handlePacket(p, context)
            }
        } catch (e: Exception) {
            this.onDecodeError(context, e.message ?: "")
        }
    }

}
