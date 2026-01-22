
package schwalbe.ventura.server

import kotlinx.serialization.Serializable

@Serializable
data class PlayerData(
    val worlds: MutableList<WorldEntry>
) {
    
    companion object {}

    @Serializable
    data class WorldEntry(
        val worldId: Long
    )

}

class Player(
    val username: String,
    val data: PlayerData,
    val connection: Server.Connection
) {}