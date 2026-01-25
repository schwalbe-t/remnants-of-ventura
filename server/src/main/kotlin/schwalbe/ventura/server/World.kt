
package schwalbe.ventura.server

import schwalbe.ventura.MAX_NUM_REQUESTED_CHUNKS
import schwalbe.ventura.net.*
import schwalbe.ventura.net.PacketType.*
import schwalbe.ventura.worlds.*
import java.util.concurrent.ConcurrentLinkedQueue

class World(val registry: WorldRegistry, val id: Long, val data: WorldData) {

    private val incoming = ConcurrentLinkedQueue<Player>()
    private val players: MutableMap<String, Player> = mutableMapOf()
    private val packetHandler = PacketHandler<Player>()

    fun transfer(player: Player) {
        this.incoming.add(player)
    }

    open fun createPlayerEntry(): PlayerData.WorldEntry
        = PlayerData.WorldEntry(this.id)

    @Synchronized
    private fun handleIncomingPlayers() {
        while (true) {
            val player: Player = this.incoming.poll() ?: break
            this.players[player.username] = player
            if (player.data.worlds.last()?.worldId != this.id) {
                player.data.worlds.add(this.createPlayerEntry())
            }
            this.registry.playerWriter.add(player)
            player.connection.outgoing.send(Packet.serialize(
                DOWN_COMPLETE_WORLD_CHANGE, Unit
            ))
        }
    }

    @Synchronized
    private fun handlePlayerPackets() {
        for (player in this.players.values) {
            this.packetHandler.handleAll(player.connection.incoming, player)
        }
    }

    fun handlePlayerDisconnect(player: Player) {
        this.players.remove(player.username)
    }

    fun handlePlayerLeaving(player: Player) {
        this.players.remove(player.username)
    }

    @Synchronized
    fun update() {
        this.handleIncomingPlayers()
        this.handlePlayerPackets()
    }

    private val constWorldInfo: Packet
        = Packet.serialize(DOWN_CONST_WORLD_INFO, data.info)

    init {
        val ph = this.packetHandler
        ph.onDecodeError = { player, error ->
            player.connection.outgoing.send(Packet.serialize(
                DOWN_GENERIC_ERROR, GenericErrorPacket(error)
            ))
        }
        ph.onPacket(UP_REQUEST_WORLD_INFO) { _: Unit, pl ->
            pl.connection.outgoing.send(this.constWorldInfo)
        }
        ph.onPacket(UP_REQUEST_CHUNK_CONTENTS) { r: RequestedChunksPacket, pl ->
            if (r.chunks.size > MAX_NUM_REQUESTED_CHUNKS) {
                return@onPacket pl.connection.outgoing.send(Packet.serialize(
                    DOWN_TAGGED_ERROR,
                    TaggedErrorPacket.TOO_MANY_CHUNKS_REQUESTED
                ))
            }
            val chunks: List<Pair<ChunkRef, ChunkData>> = r.chunks.mapNotNull {
                val d: ChunkData? = this.data.chunks[it]
                if (d == null) { null } else { it to d }
            }
            pl.connection.outgoing.send(Packet.serialize(
                DOWN_CHUNK_CONTENTS, ChunkContentsPacket(chunks)
            ))
        }
        ph.onPacket(UP_REQUEST_WORLD_LEAVE) { r: WorldChangePacket, pl ->
            pl.popWorld(this.registry)
        }
    }

}