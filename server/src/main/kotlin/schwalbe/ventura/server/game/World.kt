
package schwalbe.ventura.server.game

import schwalbe.ventura.*
import schwalbe.ventura.net.*
import schwalbe.ventura.data.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.uuid.Uuid

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
        val robotStates = this.players.values.flatMap { pl ->
                pl.data.deployedRobots.map { (id, r) ->
                    id to r.buildSharedInfo()
                }
            }.toMap()
        fun worldStateAt(observer: SerVector3, pl: Player): WorldStatePacket {
            val r: Float = WORLD_STATE_CONTENT_RADIUS
            val fPlayerStates = playerStates
                .filterValues { insideSquareRadiusXZ(it.position, observer, r) }
            val fRobotStates = robotStates
                .filterValues { insideSquareRadiusXZ(it.position, observer, r) }
            val ownedRobots = pl.data.deployedRobots
                .map { (id, r) -> id to r.buildPrivateInfo() }
                .toMap()
            return WorldStatePacket(
                fPlayerStates, fRobotStates, ownedRobots
            )
        }
        for (player in this.players.values) {
            val pos: SerVector3 = player.data.worlds.last().state.position
            player.connection.outgoing.send(Packet.serialize(
                PacketType.WORLD_STATE, worldStateAt(pos, player)
            ))
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

        ph.onPacket(PacketType.UPLOAD_SOURCE_CONTENT) { sc, pl ->
            val success: Boolean = pl.data.sourceFiles
                .set(sc.path, sc.content, sc.changeTimeMs)
            if (!success) {
                pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.INVALID_SOURCE_FILE
                ))
            } else {
                pl.connection.outgoing.send(Packet.serialize(
                    PacketType.SOURCE_CONTENT_RECEIVED, Unit
                ))
            }
        }
        ph.onPacket(PacketType.REQUEST_STORED_SOURCES) { _, pl ->
            pl.connection.outgoing.send(Packet.serialize(
                PacketType.STORED_SOURCES,
                StoredSourcesInfoPacket(
                    sources = pl.data.sourceFiles.paths.associateBy(
                        keySelector = { it },
                        valueTransform = { StoredSourcesInfoPacket.SourceInfo(
                            pl.data.sourceFiles.getChangeTime(it)
                        ) }
                    )
                )
            ))
        }

        ph.onPacket(PacketType.DEPLOY_ROBOT) { d, pl ->
            if (d.robotType.itemType != d.item.type) {
                pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.REQUESTED_ROBOT_DOES_NOT_MATCH_ITEM
                ))
                return@onPacket
            }
            if (pl.data.deployedRobots.size >= MAX_NUM_DEPLOYED_ROBOTS) {
                pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR, TaggedErrorPacket.TOO_MANY_ROBOTS
                ))
                return@onPacket
            }
            val wasRemoved: Boolean = pl.data.inventory.tryRemove(d.item)
            if (!wasRemoved) {
                pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.REQUESTED_ROBOT_NOT_IN_INVENTORY
                ))
                return@onPacket
            }
            val position = pl.data.worlds.last().state.position
            val robot = Robot(d.robotType, d.item, position)
            pl.data.deployedRobots[robot.id] = robot
            pl.connection.outgoing.send(Packet.serialize(
                PacketType.ROBOT_DEPLOYED,
                robot.id
            ))
        }
        fun getRobotOrError(robotId: Uuid, player: Player): Robot? {
            val robot: Robot? = player.data.deployedRobots[robotId]
            if (robot != null) { return robot }
            player.connection.outgoing.send(Packet.serialize(
                PacketType.TAGGED_ERROR, TaggedErrorPacket.NOT_ROBOT_OWNER
            ))
            return null
        }
        ph.onPacket(PacketType.DESTROY_ROBOT) { robotId, pl ->
            val robot: Robot = getRobotOrError(robotId, pl) ?: return@onPacket
            pl.data.deployedRobots.remove(robotId)
            val inv: Inventory = pl.data.inventory
            inv.add(robot.item)
            for (attached in robot.attachments.asSequence().filterNotNull()) {
                inv.add(attached)
            }
            pl.connection.outgoing.send(Packet.serialize(
                PacketType.ROBOT_DESTROYED, robotId
            ))
        }
        ph.onPacket(PacketType.START_ROBOT) { robotId, pl ->
            val robot = getRobotOrError(robotId, pl) ?: return@onPacket
            robot.start()
        }
        ph.onPacket(PacketType.PAUSE_ROBOT) { robotId, pl ->
            val robot = getRobotOrError(robotId, pl) ?: return@onPacket
            robot.pause()
        }
        ph.onPacket(PacketType.UNPAUSE_ROBOT) { robotId, pl ->
            val robot = getRobotOrError(robotId, pl) ?: return@onPacket
            robot.unpause()
        }
        ph.onPacket(PacketType.STOP_ROBOT) { robotId, pl ->
            val robot = getRobotOrError(robotId, pl) ?: return@onPacket
            robot.stop()
        }
        ph.onPacket(PacketType.SET_ROBOT_ATTACHMENT) { at, pl ->
            val robot = getRobotOrError(at.robotId, pl) ?: return@onPacket
            if (at.attachmentId !in robot.attachments.indices) {
                pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.ATTACHMENT_IDX_OOB
                ))
                return@onPacket
            }
            val attached: Item? = at.attachedItem
            if (attached == null) {
                val detached: Item = robot.attachments[at.attachmentId]
                    ?: return@onPacket
                robot.attachments[at.attachmentId] = null
                robot.reset()
                pl.data.inventory.add(detached)
                return@onPacket
            }
            val wasRemoved: Boolean = pl.data.inventory.tryRemove(attached)
            if (!wasRemoved) {
                pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.ATTACHMENT_NOT_IN_INVENTORY
                ))
                return@onPacket
            }
            val replaced: Item? = robot.attachments[at.attachmentId]
            if (replaced != null) { pl.data.inventory.add(replaced) }
            robot.attachments[at.attachmentId] = attached
            robot.reset()
        }
        ph.onPacket(PacketType.SET_ROBOT_SOURCES) { src, pl ->
            if (src.sourceFiles.size > MAX_NUM_ROBOT_SOURCE_FILES) {
                pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.TOO_MANY_ROBOT_SOURCE_FILES
                ))
                return@onPacket
            }
            val robot = getRobotOrError(src.robotId, pl) ?: return@onPacket
            robot.sourceFiles = src.sourceFiles
            robot.reset()
            src.sourceFiles.forEach {
                if (!pl.data.sourceFiles.touch(it)) {
                    pl.connection.outgoing.send(Packet.serialize(
                        PacketType.TAGGED_ERROR,
                        TaggedErrorPacket.TOO_MANY_PLAYER_SOURCE_FILES
                    ))
                }
            }
        }
        ph.onPacket(PacketType.SET_ROBOT_NAME) { n, pl ->
            if (n.newName.length > ROBOT_NAME_MAX_LEN) {
                pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.ROBOT_NAME_TOO_LONG
                ))
                return@onPacket
            }
            val robot = getRobotOrError(n.robotId, pl) ?: return@onPacket
            robot.name = n.newName
        }
        ph.onPacket(PacketType.REQUEST_ROBOT_LOGS) { robotId, pl ->
            val robot = getRobotOrError(robotId, pl) ?: return@onPacket
            pl.connection.outgoing.send(Packet.serialize(
                PacketType.ROBOT_LOGS,
                RobotLogsPacket(robotId, robot.buildLogString())
            ))
        }
    }

}