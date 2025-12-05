
package schwalbe.ventura.server

import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.datetime.*

abstract class World(val id: Int) {

    private val incoming = ConcurrentLinkedQueue<Player>()
    private val players: MutableMap<String, Player> = mutableMapOf()
    val creationTime: Instant = Clock.System.now()

    fun transfer(player: Player) {
        this.incoming.add(player)
    }

    abstract fun createPlayerEntry(): PlayerData.WorldEntry

    @Synchronized
    private fun handleIncomingPlayers() {
        while (true) {
            val player: Player? = this.incoming.poll()
            if (player == null) { break }
            this.players[player.username] = player
            player.data.worlds.add(this.createPlayerEntry())
            player.saveAsap()
        }
    }

    @Synchronized
    private fun handlePlayerPackets(registry: WorldRegistry) {
        
    }

    abstract fun updateState(registry: WorldRegistry)

    fun update(registry: WorldRegistry) {
        this.handleIncomingPlayers()
        this.handlePlayerPackets(registry)
        this.updateState(registry)
    }

}