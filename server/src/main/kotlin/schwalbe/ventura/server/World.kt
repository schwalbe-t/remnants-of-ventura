
package schwalbe.ventura.server

import schwalbe.ventura.net.*
import java.util.concurrent.ConcurrentLinkedQueue

abstract class World(val registry: WorldRegistry) {

    val id: Long = registry.allocateWorld()
    private val incoming = ConcurrentLinkedQueue<Player>()
    protected val players: MutableMap<String, Player> = mutableMapOf()
    protected val packetHandler = PacketHandler<Player>()

    init {
        this.packetHandler.onDecodeError = { player, error ->
            player.connection.outgoing.send(Packet.serialize(
                PacketType.DOWN_GENERIC_ERROR, GenericErrorPacket(error)
            ))
        }
    }

    fun transfer(player: Player) {
        this.incoming.add(player)
    }

    open fun createPlayerEntry(): PlayerData.WorldEntry
        = PlayerData.WorldEntry(this.id)

    @Synchronized
    private fun handleIncomingPlayers() {
        while (true) {
            val player: Player? = this.incoming.poll()
            if (player == null) { break }
            this.players[player.username] = player
            val addEntry: Boolean = player.data.worlds.size == 0
                || player.data.worlds.last().worldId != this.id
            if (addEntry) {
                player.data.worlds.add(this.createPlayerEntry())
            }
            this.registry.playerWriter.add(player)
        }
    }

    @Synchronized
    private fun handlePlayerPackets() {
        for (player in this.players.values) {
            this.packetHandler.handleAll(player.connection.incoming, player)
        }
    }

    open fun updateState() {}

    @Synchronized
    fun update() {
        this.handleIncomingPlayers()
        this.handlePlayerPackets()
        this.updateState()
    }

    fun handlePlayerDisconnect(player: Player) {
        this.players.remove(player.username)
    }

}