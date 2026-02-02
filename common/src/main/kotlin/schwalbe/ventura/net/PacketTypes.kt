
package schwalbe.ventura.net

import kotlinx.serialization.Serializable
import schwalbe.ventura.data.ChunkData
import schwalbe.ventura.data.ChunkRef
import schwalbe.ventura.data.ConstWorldInfo
import schwalbe.ventura.data.Item
import kotlin.uuid.Uuid

enum class PacketDirection { UP, DOWN }

data class PacketType<P>(
    val ordinal: Int, val direction: PacketDirection
) { companion object {

    private var nextId: Int = 0
    private fun pollNextPacketId(): Int {
        this.nextId += 1
        return this.nextId - 1
    }

    private fun <P> up() = PacketType<P>(
        this.pollNextPacketId(), PacketDirection.UP
    )
    private fun <P> down() = PacketType<P>(
        this.pollNextPacketId(), PacketDirection.DOWN
    )


    val GENERIC_ERROR               = down<GenericErrorPacket>()
    val TAGGED_ERROR                = down<TaggedErrorPacket>()

    val CREATE_ACCOUNT              = up<AccountCredPacket>()
    val CREATE_ACCOUNT_SUCCESS      = down<Unit>()
    val CREATE_SESSION              = up<AccountCredPacket>()
    val CREATE_SESSION_SUCCESS      = down<SessionTokenPacket>()
    val LOGIN_SESSION               = up<SessionCredPacket>()
    val LOGIN_SESSION_SUCCESS       = down<Unit>()

    val BEGIN_WORLD_CHANGE          = down<Unit>()
    val COMPLETE_WORLD_CHANGE       = down<WorldEntryPacket>()
    val REQUEST_WORLD_INFO          = up<Unit>()
    val CONST_WORLD_INFO            = down<ConstWorldInfo>()
    val REQUEST_CHUNK_CONTENTS      = up<RequestedChunksPacket>()
    val CHUNK_CONTENTS              = down<ChunkContentsPacket>()
    val REQUEST_WORLD_LEAVE         = up<Unit>()

    val PLAYER_STATE                = up<SharedPlayerInfo>()
    val WORLD_STATE                 = down<WorldStatePacket>()

    val REQUEST_INVENTORY_CONTENTS  = up<Unit>()
    val INVENTORY_CONTENTS          = down<InventoryContentsPacket>()

    val NUM_PACKET_TYPES: Int = this.pollNextPacketId()

} }

@Serializable
data class GenericErrorPacket(val message: String)

@Serializable
enum class TaggedErrorPacket {
    // username exists or username/password too long
    // (UI can safely ignore the latter)
    INVALID_ACCOUNT_PARAMS,
    // invalid username/password
    INVALID_ACCOUNT_CREDS,
    // client on session creation cooldown
    SESSION_CREATION_COOLDOWN,
    // invalid username/token
    INVALID_SESSION_CREDS,
    // account already online
    ACCOUNT_ALREADY_ONLINE,

    // chunk data request asked for too many chunks
    TOO_MANY_CHUNKS_REQUESTED
}


@Serializable
data class AccountCredPacket(val username: String, val password: String)

@Serializable
data class SessionTokenPacket(val token: Uuid)

@Serializable
data class SessionCredPacket(val username: String, val token: Uuid)


@Serializable
data class WorldEntryPacket(val position: SerVector3)

@Serializable
data class RequestedChunksPacket(val chunks: List<ChunkRef>)

@Serializable
data class ChunkContentsPacket(val chunks: List<Pair<ChunkRef, ChunkData>>)


@Serializable
data class SharedPlayerInfo(
    val position: SerVector3,
    val rotation: Float,
    val animation: Animation
) {
    @Serializable
    enum class Animation {
        IDLE, WALK, SQUAT
    }
}

@Serializable
data class WorldStatePacket(
    val players: Map<String, SharedPlayerInfo>
)


@Serializable
data class InventoryContentsPacket(
    val itemCounts: Map<Item, Int>
)