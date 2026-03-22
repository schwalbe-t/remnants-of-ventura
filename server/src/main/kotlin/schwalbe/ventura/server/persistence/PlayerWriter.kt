
package schwalbe.ventura.server.persistence

import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.serializer
import kotlinx.serialization.cbor.Cbor
import schwalbe.ventura.server.game.Player
import schwalbe.ventura.server.game.PlayerData
import schwalbe.ventura.server.QueuedWorker

class SerializedPlayerData(
    val username: String,
    val bytes: ByteArray
)

fun Player.serialize(): SerializedPlayerData = SerializedPlayerData(
    this.username,
    Cbor.encodeToByteArray(serializer<PlayerData>(), this.data)
)

class PlayerWriter : QueuedWorker<SerializedPlayerData>() {

    override fun completeTasks(tasks: List<SerializedPlayerData>) {
        transaction {
            for (player in tasks) {
                Account.writePlayerData(player.username, player.bytes)
            }
        }
    }

}