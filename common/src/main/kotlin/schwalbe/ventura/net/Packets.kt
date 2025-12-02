
package schwalbe.ventura.net

import kotlinx.serialization.Serializable

@Serializable
enum class PacketType {
    UP_ECHO,
    DOWN_ECHO
}

data class Packet(val type: PacketType, val payload: ByteArray)


@Serializable
data class EchoPacket(val content: String)
