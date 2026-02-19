
package schwalbe.ventura.net

import schwalbe.ventura.data.*
import kotlinx.serialization.Serializable
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

    val UPLOAD_SOURCE_CONTENT       = up<UploadSourceContentsPacket>()
    val SOURCE_CONTENT_RECEIVED     = down<Unit>()
    val REQUEST_STORED_SOURCES      = up<Unit>()
    val STORED_SOURCES              = down<StoredSourcesInfoPacket>()

    val DEPLOY_ROBOT                = up<RobotDeploymentPacket>()
    val ROBOT_DEPLOYED              = down<Uuid>()
    val DESTROY_ROBOT               = up<Uuid>()
    val ROBOT_DESTROYED             = down<Uuid>()
    val START_ROBOT                 = up<Uuid>()
    val PAUSE_ROBOT                 = up<Uuid>()
    val UNPAUSE_ROBOT               = up<Uuid>()
    val STOP_ROBOT                  = up<Uuid>()
    val SET_ROBOT_ATTACHMENT        = up<RobotAttachmentChangePacket>()
    val SET_ROBOT_SOURCES           = up<RobotSourceFilesChangePacket>()
    val SET_ROBOT_NAME              = up<RobotNameChangePacket>()
    val REQUEST_ROBOT_LOGS          = up<Uuid>()
    val ROBOT_LOGS                  = down<RobotLogsPacket>()

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
    TOO_MANY_CHUNKS_REQUESTED,

    // requested robot deployment failed due to non-matching item
    REQUESTED_ROBOT_DOES_NOT_MATCH_ITEM,
    // requested robot deployment failed due to item not being in inventory
    REQUESTED_ROBOT_NOT_IN_INVENTORY,
    // requested robot deployment failed because max number reached
    TOO_MANY_ROBOTS,
    // attempt to manipulate robot failed because robot does not exist or is
    // not owned by the calling player
    NOT_ROBOT_OWNER,
    // attempt to change attachment failed because the attachment index is OOB
    ATTACHMENT_IDX_OOB,
    // attempt to change attachment failed due to item not being in inventory
    ATTACHMENT_NOT_IN_INVENTORY,
    // attempt to configure robot source files failed because too many given
    TOO_MANY_ROBOT_SOURCE_FILES,
    // provided robot name is too long
    ROBOT_NAME_TOO_LONG
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
data class SharedRobotInfo(
    val name: String,
    val item: Item,
    val status: RobotStatus,
    val position: SerVector3,
    val rotation: Float
)

@Serializable
data class PrivateRobotInfo(
    val attachments: List<Item?>,
    val sourceFiles: List<String>,
    val fracHealth: Float,
    val fracMemUsage: Float,
    val fracCpuUsage: Float
)

@Serializable
data class WorldStatePacket(
    val players: Map<String, SharedPlayerInfo>,
    val allRobots: Map<Uuid, SharedRobotInfo>,
    val ownedRobots: Map<Uuid, PrivateRobotInfo>
)


@Serializable
data class InventoryContentsPacket(val itemCounts: Map<Item, Int>)


@Serializable
data class RobotDeploymentPacket(
    val robotType: RobotType,
    val item: Item
)

@Serializable
data class UploadSourceContentsPacket(
    val path: String,
    val content: String,
    val changeTimeMs: Long
)

@Serializable
data class StoredSourcesInfoPacket(val sources: Map<String, SourceInfo>) {
    @Serializable
    data class SourceInfo(val lastChangeTimeMs: Long)
}


@Serializable
data class RobotLogsPacket(
    val robotId: Uuid,
    val logs: String
)

@Serializable
data class RobotAttachmentChangePacket(
    val robotId: Uuid,
    val attachmentId: Int,
    val attachedItem: Item?
)

@Serializable
data class RobotSourceFilesChangePacket(
    val robotId: Uuid,
    val sourceFiles: List<String>
)

@Serializable
data class RobotNameChangePacket(
    val robotId: Uuid,
    val newName: String
)