
package schwalbe.ventura.server

import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.datetime.*

abstract class World(val id: Long) {

    private val incoming = ConcurrentLinkedQueue<Player>()
    private val players: MutableMap<String, Player> = mutableMapOf()
    val creationTime: Instant = Clock.System.now()

    fun transfer(player: Player) {
        this.incoming.add(player)
    }

    open fun createPlayerEntry(): PlayerData.WorldEntry
        = PlayerData.WorldEntry(this.id)

    @Synchronized
    private fun handleIncomingPlayers(registry: WorldRegistry) {
        while (true) {
            val player: Player? = this.incoming.poll()
            if (player == null) { break }
            this.players[player.username] = player
            val addEntry: Boolean = player.data.worlds.size == 0
                || player.data.worlds.last().worldId != this.id
            if (addEntry) {
                player.data.worlds.add(this.createPlayerEntry())
            }
            registry.playerWriter.add(player)
        }
    }

    @Synchronized
    private fun handlePlayerPackets(registry: WorldRegistry) {
        
    }

    open fun updateState(registry: WorldRegistry) {}

    fun update(registry: WorldRegistry) {
        this.handleIncomingPlayers(registry)
        this.handlePlayerPackets(registry)
        this.updateState(registry)
    }

    fun handlePlayerDisconnect(player: Player) {
        this.players.remove(player.username)
    }

}