
package schwalbe.ventura.net

import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.serializer

class PacketHandler<C>(val receivingDir: PacketDirection) {

    inner class Ref(var f: (ByteArray, C, Ref) -> Unit = { _, _, _ -> })

    companion object {
        fun <C> receiveUpPackets() = PacketHandler<C>(PacketDirection.UP)
        fun <C> receiveDownPackets() = PacketHandler<C>(PacketDirection.DOWN)
    }


    val handlers: MutableMap<Short, MutableList<Ref>>
        = mutableMapOf()
    var onDecodeError: (C, String) -> Unit = { _, _ -> }

    inline fun <reified P> onPacketRef(
        packetType: PacketType<P>, crossinline handler: (P, C, Ref) -> Unit
    ): Ref {
        require(packetType.direction == this.receivingDir) {
            "Packet handler was configured to receive packets of direction " +
                    "'${this.receivingDir.name}', but a packet type with direction " +
                    "'${packetType.direction.name}' was attempted to be registered"
        }
        val err: (C, String) -> Unit = this.onDecodeError
        val ref = Ref { rpl: ByteArray, ctx: C, self: Ref ->
            val decPayload: P
            try {
                decPayload = Cbor.decodeFromByteArray(serializer<P>(), rpl)
            } catch (e: Exception) {
                err(ctx, e.message ?: "")
                return@Ref
            }
            try {
                handler(decPayload, ctx, self)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val packetTypeId: Short = packetType.ordinal.toShort()
        val handlers: MutableList<Ref>
            = this.handlers.getOrPut(packetTypeId) { mutableListOf() }
        handlers.add(ref)
        return ref
    }

    inline fun <reified P> onPacket(
        packetType: PacketType<P>, crossinline handler: (P, C) -> Unit
    ): PacketHandler<C> {
        this.onPacketRef(packetType) { p, c, _ -> handler(p, c) }
        return this
    }

    inline fun <reified P> onPacketOnce(
        packetType: PacketType<P>, crossinline handler: (P, C) -> Unit
    ): PacketHandler<C> {
        this.onPacketRef(packetType) { p, c, r ->
            handler(p, c)
            this.remove(r)
        }
        return this
    }

    fun remove(handlerRef: Ref) {
        for (handlers in this.handlers) {
            handlers.value.remove(handlerRef)
        }
    }

    fun handlePacket(packet: Packet, context: C) {
        val handlers = this.handlers[packet.type] ?: return
        handlers.toList().forEach { it.f(packet.payload, context, it) }
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
