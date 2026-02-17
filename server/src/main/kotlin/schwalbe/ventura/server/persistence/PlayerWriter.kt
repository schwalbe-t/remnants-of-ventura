
package schwalbe.ventura.server.persistence

import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.serializer
import kotlinx.serialization.cbor.Cbor
import schwalbe.ventura.server.game.Player
import schwalbe.ventura.server.game.PlayerData
import schwalbe.ventura.server.QueuedWorker

class PlayerWriter : QueuedWorker<Player>() {

    override fun completeTasks(tasks: List<Player>) {
        transaction {
            for (player in tasks) {
                val bytes: ByteArray = Cbor.encodeToByteArray(
                    serializer<PlayerData>(), player.data
                )
                Account.writePlayerData(player.username, bytes)
            }
        }
    }

}