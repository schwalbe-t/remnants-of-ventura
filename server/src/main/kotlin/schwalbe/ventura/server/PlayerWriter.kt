
package schwalbe.ventura.server

import schwalbe.ventura.server.database.AccountsTable
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
import java.util.concurrent.locks.ReentrantLock
import kotlinx.serialization.serializer
import kotlinx.serialization.cbor.Cbor
import kotlin.concurrent.withLock

class PlayerWriter {

    private val lock = ReentrantLock()
    private var players = mutableListOf<Player>()
    private val hasPlayer = this.lock.newCondition()

    fun add(player: Player) {
        this.lock.withLock {
            this.players.add(player)
            this.hasPlayer.signal()
        }
    }

    fun writePlayers() {
        while (true) {
            val saving: List<Player>
            this.lock.withLock {
                while (this.players.isEmpty()) {
                    this.hasPlayer.await()
                }
                saving = this.players
                this.players = mutableListOf<Player>()
            }
            transaction {
                for (player in saving) {
                    val bytes: ByteArray = Cbor.encodeToByteArray(
                        serializer<PlayerData>(), player.data
                    )
                    Account.writePlayerData(player.username, bytes)
                }
            }
        }
    }

}