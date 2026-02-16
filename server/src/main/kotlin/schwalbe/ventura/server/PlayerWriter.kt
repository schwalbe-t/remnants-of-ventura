
package schwalbe.ventura.server

import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.serializer
import kotlinx.serialization.cbor.Cbor

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