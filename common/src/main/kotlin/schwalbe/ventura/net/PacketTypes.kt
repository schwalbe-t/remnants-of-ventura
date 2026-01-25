
package schwalbe.ventura.net

import kotlinx.serialization.Serializable
import schwalbe.ventura.worlds.ChunkData
import schwalbe.ventura.worlds.ChunkRef
import kotlin.uuid.Uuid

@Serializable
enum class PacketType {
    DOWN_GENERIC_ERROR,             // GenericErrorPacket
    DOWN_TAGGED_ERROR,              // TaggedErrorPacket

    UP_CREATE_ACCOUNT,              // AccountCredPacket 
    DOWN_CREATE_ACCOUNT_SUCCESS,    // Unit
    UP_CREATE_SESSION,              // AccountCredPacket
    DOWN_CREATE_SESSION_SUCCESS,    // SessionTokenPacket
    UP_LOGIN_SESSION,               // SessionCredPacket
    DOWN_LOGIN_SESSION_SUCCESS,     // Unit

    DOWN_BEGIN_WORLD_CHANGE,
    DOWN_COMPLETE_WORLD_CHANGE,     // Unit
    UP_REQUEST_WORLD_INFO,          // Unit
    DOWN_CONST_WORLD_INFO,          // ConstWorldInfo
    UP_REQUEST_CHUNK_CONTENTS,      // RequestedChunksPacket
    DOWN_CHUNK_CONTENTS,            // ChunkContentsPacket
    UP_REQUEST_WORLD_LEAVE          // Unit
}

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
data class RequestedChunksPacket(val chunks: List<ChunkRef>)

@Serializable
data class ChunkContentsPacket(val chunks: List<Pair<ChunkRef, ChunkData>>)

@Serializable
data class WorldChangePacket(val worldId: Long)