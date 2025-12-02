
package schwalbe.ventura.net

import kotlinx.serialization.Serializable

@Serializable
enum class PacketType {
    UP_ECHO,
    DOWN_ECHO
}

@Serializable
data class EchoPacket(val content: String)