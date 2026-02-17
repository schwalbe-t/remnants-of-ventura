
package schwalbe.ventura.server.game

import schwalbe.ventura.MAX_NUM_REQUESTED_CHUNKS
import schwalbe.ventura.WORLD_STATE_CONTENT_RADIUS
import schwalbe.ventura.net.*
import schwalbe.ventura.data.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs

private fun insideSquareRadiusXZ(p: SerVector3, center: SerVector3, r: Float)
    = maxOf(abs(p.x - center.x), abs(p.y - center.y)) <= r

class World(val registry: WorldRegistry, val id: Long, val data: WorldData) {

    private val incoming = ConcurrentLinkedQueue<Player>()
    private val players: MutableMap<String, Player> = mutableMapOf()
    private val packetHandler = PacketHandler.receiveUpPackets<Player>()

    fun transfer(player: Player) {
        this.incoming.add(player)
    }

    fun createPlayerEntry(): PlayerData.WorldEntry
        = PlayerData.WorldEntry(
            worldId = this.id,
            state = SharedPlayerInfo(
                position = SerVector3(0f, 0f, 0f),
                rotation = 0f,
                animation = SharedPlayerInfo.Animation.IDLE
            )
        )

    @Synchronized
    private fun handleIncomingPlayers() {
        while (true) {
            val player: Player = this.incoming.poll() ?: break
            this.players[player.username] = player
            if (player.data.worlds.last()?.worldId != this.id) {
                player.data.worlds.add(this.createPlayerEntry())
            }
            this.registry.workers.playerWriter.add(player)
            player.connection.outgoing.send(Packet.serialize(
                PacketType.COMPLETE_WORLD_CHANGE,
                WorldEntryPacket(
                    position = player.data.worlds.last().state.position
                )
            ))
        }
    }

    @Synchronized
    private fun handlePlayerPackets() {
        for (player in this.players.values) {
            this.packetHandler.handleAll(player.connection.incoming, player)
        }
    }

    @Synchronized
    fun handlePlayerDisconnect(player: Player) {
        this.players.remove(player.username)
    }

    @Synchronized
    fun handlePlayerLeaving(player: Player) {
        this.players.remove(player.username)
    }
    
    private fun updateState() {
        for (player in this.players.values) {
            player.updateState(this)
        }
    }

    private fun sendWorldStatePacket() {
        val playerStates = this.players.map { (name, pl) ->
                name to pl.data.worlds.last().state
            }.toMap()
        fun worldStateAt(observer: SerVector3): WorldStatePacket {
            val r: Float = WORLD_STATE_CONTENT_RADIUS
            val fPlayerStates = playerStates
                .filterValues { insideSquareRadiusXZ(it.position, observer, r) }
            return WorldStatePacket(
                fPlayerStates,
                // TODO! send actual robot state
                allRobots = mapOf(),
                ownedRobots = mapOf()
            )
        }
        for (player in this.players.values) {
            val pos: SerVector3 = player.data.worlds.last().state.position
            val ws = Packet.serialize(PacketType.WORLD_STATE, worldStateAt(pos))
            player.connection.outgoing.send(ws)
        }
    }

    @Synchronized
    fun update() {
        this.handleIncomingPlayers()
        this.handlePlayerPackets()
        this.updateState()
        this.sendWorldStatePacket()
    }

    private val constWorldInfo: Packet
        = Packet.serialize(PacketType.CONST_WORLD_INFO, data.info)

    init {
        val ph = this.packetHandler
        ph.onDecodeError = { player, error ->
            player.connection.outgoing.send(Packet.serialize(
                PacketType.GENERIC_ERROR, GenericErrorPacket(error)
            ))
        }
        ph.onPacket(PacketType.REQUEST_WORLD_INFO) { _, pl ->
            pl.connection.outgoing.send(this.constWorldInfo)
        }
        ph.onPacket(PacketType.REQUEST_CHUNK_CONTENTS) { r, pl ->
            if (r.chunks.size > MAX_NUM_REQUESTED_CHUNKS) {
                return@onPacket pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.TOO_MANY_CHUNKS_REQUESTED
                ))
            }
            val chunks: List<Pair<ChunkRef, ChunkData>> = r.chunks.mapNotNull {
                val data = this.data.chunks[it] ?: return@mapNotNull null
                it to data
            }
            if (chunks.isEmpty()) { return@onPacket }
            pl.connection.outgoing.send(Packet.serialize(
                PacketType.CHUNK_CONTENTS, ChunkContentsPacket(chunks)
            ))
        }
        ph.onPacket(PacketType.REQUEST_WORLD_LEAVE) { _, pl ->
            pl.popWorld(this.registry)
        }
        ph.onPacket(PacketType.PLAYER_STATE) { pi, pl ->
            pl.data.worlds.last().state = pi
        }
        ph.onPacket(PacketType.REQUEST_INVENTORY_CONTENTS) { _, pl ->
            pl.connection.outgoing.send(Packet.serialize(
                PacketType.INVENTORY_CONTENTS,
                InventoryContentsPacket(pl.data.inventory.itemCounts)
            ))
        }
    }

}