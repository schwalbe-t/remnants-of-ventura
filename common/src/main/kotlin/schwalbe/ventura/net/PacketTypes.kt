
package schwalbe.ventura.net

import kotlinx.serialization.Serializable
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
    DOWN_LOGIN_SESSION_SUCCESS      // Unit
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
    // invalid username/token
    INVALID_SESSION_CREDS,
    // account already online
    ACCOUNT_ALREADY_ONLINE
}

@Serializable
data class AccountCredPacket(val username: String, val password: String)

@Serializable
data class SessionTokenPacket(val token: Uuid)

@Serializable
data class SessionCredPacket(val username: String, val token: Uuid)