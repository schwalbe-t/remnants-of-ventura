
package schwalbe.ventura.server.game

import schwalbe.ventura.*
import schwalbe.ventura.net.*
import schwalbe.ventura.data.*
import schwalbe.ventura.server.persistence.serialize
import schwalbe.ventura.utils.GroundColorReader
import schwalbe.ventura.utils.SerVector3
import schwalbe.ventura.utils.insideSquareRadiusXZ
import schwalbe.ventura.utils.toVector3f
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.uuid.Uuid

class StaticWorldData(
    val world: SerializedWorld,
    val groundColor: GroundColorReader
)

class World(
    val registry: WorldRegistry,
    val name: String,
    val id: Uuid,
    val static: StaticWorldData
) {

    companion object {
        const val MAX_ROBOT_REPAIR_DIST: Float = 5f
        const val DROPPED_ITEM_SPREAD: Float = 0.5f
        const val ITEM_DISPENSER_SPREAD: Float = 1f
    }



    private val incoming = ConcurrentLinkedQueue<Player>()
    private val mutPlayers: MutableMap<String, Player> = mutableMapOf()
    val players: Map<String, Player> = this.mutPlayers
    private val packetHandler = PacketHandler.receiveUpPackets<Player>()

    fun transfer(player: Player) {
        this.incoming.add(player)
    }

    fun createPlayerEntry(style: PersonStyle): PlayerData.WorldEntry
        = PlayerData.WorldEntry(
            worldId = this.id,
            state = SharedPlayerInfo(
                position = this.static.world.startPosition,
                rotation = 0f,
                animation = SharedPersonAnimation.IDLE,
                style = style
            )
        )

    @Synchronized
    fun hasPlayers(): Boolean = this.players.isNotEmpty()

    @Synchronized
    private fun prepareIncomingPlayer(player: Player) {
        val playerPosition = player.data.worlds.last().state.position
        for (robot in player.data.deployedRobots.values) {
            val newPos = Robot.allocatePosition(playerPosition, this)
                ?: playerPosition
            robot.resetPosition(newPos)
        }
    }

    @Synchronized
    private fun handleIncomingPlayers() {
        while (true) {
            val player: Player = this.incoming.poll() ?: break
            this.mutPlayers[player.username] = player
            this.prepareIncomingPlayer(player)
            this.registry.services.playerWriter.add(player.serialize())
            player.connection.outgoing.send(Packet.serialize(
                PacketType.COMPLETE_WORLD_CHANGE,
                WorldInfoPacket(
                    worldId = this.id,
                    isMainWorld = this.name == registry.baseWorldName,
                    worldInfo = this.static.world.info,
                    position = player.data.worlds.last().state.position,
                    playerStyle = player.data.style
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
        this.mutPlayers.remove(player.username)
    }

    @Synchronized
    fun handlePlayerLeaving(player: Player) {
        this.mutPlayers.remove(player.username)
    }

    @Synchronized
    fun handleWorldClosing() {
        for (player in this.players.values) {
            player.popWorld(this.registry)
        }
    }



    val chunkCollisions: ChunkCollisions
        = ChunkCollisions(this.static.world.chunks)
    val enemyRobots: MutableMap<Uuid, EnemyRobot> = mutableMapOf()
    val groundItems: MutableMap<Uuid, GroundItem> = mutableMapOf()
    val triggerables: Triggerables = Triggerables(this.static.world)

    fun tileIsOccupied(tx: Int, tz: Int): Boolean {
        if (this.chunkCollisions[tx, tz]) { return true }
        fun robotOccupiesTile(r: Robot) = r.tileX == tx && r.tileZ == tz
        val byPlayerRobot: Boolean = this.players.values
            .any { it.data.deployedRobots.values.any(::robotOccupiesTile) }
        if (byPlayerRobot) { return true }
        val byEnemyRobot: Boolean = this.enemyRobots.values
            .any(::robotOccupiesTile)
        return byEnemyRobot
    }

    @Synchronized
    fun spawnItem(
        origin: SerVector3, item: Item, count: Int = 1,
        ownerName: String? = null, spread: Float = DROPPED_ITEM_SPREAD
    ) {
        val angle: Float = (Math.random() * 2.0 * PI).toFloat()
        val pos = SerVector3(
            origin.x + cos(angle) * spread, origin.y,
            origin.z + sin(angle) * spread
        )
        val item = GroundItem(pos, item, count, ownerName)
        this.groundItems[item.id] = item
    }

    private val dispensers: List<ObjectInstance>
        = this.static.world.chunks.flatMap {
            it.value.instances.filter { o -> ObjectProp.ItemDispenser in o }
        }

    @Synchronized
    private fun updateItemDispensers() {
        for (dispenser in this.dispensers) {
            val dPos = dispenser[ObjectProp.Position]
            val s = dispenser[ObjectProp.ItemDispenser]
            for (player in this.players.values) {
                if (player.username in s.givenTo) { continue }
                val plPos = player.data.worlds.last().state.position
                val dist = maxOf(abs(plPos.x - dPos.x), abs(plPos.z - dPos.z))
                if (dist > s.dist) { continue }
                this.spawnItem(
                    dPos, s.item, s.count, player.username,
                    ITEM_DISPENSER_SPREAD
                )
                s.givenTo.add(player.username)
            }
        }
    }

    private val spawners: List<ObjectInstance>
        = this.static.world.chunks.flatMap {
            it.value.instances.filter { o -> ObjectProp.EnemySpawner in o }
        }

    @Synchronized
    private fun updateEnemySpawners() {
        for (spawner in this.spawners) {
            val sPos = spawner[ObjectProp.Position]
            val s = spawner[ObjectProp.EnemySpawner]
            s.defeatedCount += s.created.count { it !in this.enemyRobots.keys }
            s.created.removeIf { it !in this.enemyRobots.keys }
            if (s.defeatedCount + s.created.size >= s.totalCount) { continue }
            val pDist: Float = this.players.values
                .minOfOrNull {
                    val pPos = it.data.worlds.last().state.position
                    maxOf(abs(pPos.x - sPos.x), abs(pPos.z - sPos.z))
                }
                ?: Float.MAX_VALUE
            val isActive = pDist <= s.dist
            if (!isActive) {
                s.created.forEach { this.enemyRobots.remove(it) }
                s.created.clear()
                s.defeatedCount = 0
                continue
            }
            if (s.created.size >= s.maxConcurrent) { continue }
            val now = System.currentTimeMillis()
            if (now < s.lastSpawn + s.interval) { continue }
            s.lastSpawn = now
            val cfg: EnemyRobotConfig = EnemyRobotConfig.entries
                .firstOrNull { it.name == s.configName } ?: continue
            val rPos = Robot.allocatePosition(sPos, this) ?: continue
            val spawned = EnemyRobot(cfg, rPos)
            this.enemyRobots[spawned.id] = spawned
            s.created.add(spawned.id)
        }
    }

    @Synchronized
    private fun updateEnemyRobots() {
        for (robot in this.enemyRobots.values.filter { it.health <= 0f }) {
            robot.config.lootTable.generateLoot().forEach {
                this.spawnItem(robot.position, it)
            }
            this.enemyRobots.remove(robot.id)
        }
        for (robot in this.enemyRobots.values) {
            robot.update(this)
        }
    }

    @Synchronized
    private fun updateGroundItems() {
        val lastCreate = System.currentTimeMillis() - GroundItem.DESPAWN_DELAY
        this.groundItems.values.removeIf {
            if (it.creationTime <= lastCreate) {
                return@removeIf true
            }
            val owners: Iterable<String> = it.ownerName?.let(::listOf)
                ?: this.players.keys
            for (username in owners) {
                val player = this.players[username] ?: continue
                val pp = player.data.worlds.last().state.position
                val d = hypot(pp.x - it.position.x, pp.z - it.position.z)
                if (d > GroundItem.MAX_PICK_UP_DIST) { continue }
                player.data.inventory.add(it.item, it.count)
                return@removeIf true
            }
            false
        }
    }

    @Synchronized
    private fun updateState() {
        this.chunkCollisions.updateDynamic(this)
        for (player in this.players.values) {
            player.updateState(this)
        }
        this.updateItemDispensers()
        this.updateEnemySpawners()
        this.updateEnemyRobots()
        this.updateGroundItems()
        this.triggerables.update(this)
    }

    @Synchronized
    private fun sendWorldStatePacket() {
        val playerStates = this.players.map { (name, pl) ->
                name to pl.data.worlds.last().state
            }.toMap()
        val playerRobotStates = this.players.values.flatMap { pl ->
                pl.data.deployedRobots.map { (id, r) ->
                    id to r.buildSharedInfo()
                }
            }.toMap()
        val enemyRobotStates = this.enemyRobots.values
            .associateBy({ it.id }, { it.buildSharedInfo() })
        val robotStates = playerRobotStates + enemyRobotStates
        val groundItems = this.groundItems
        val triggeredObjects = this.triggerables.objects.asSequence()
            .filter { (_, obj) -> obj.isTriggered }
            .map { (name, _) -> name }
            .toSet()
        val now: Long = System.currentTimeMillis()
        fun worldStateAt(observer: SerVector3, pl: Player): WorldStatePacket {
            val r: Float = WORLD_STATE_CONTENT_RADIUS
            val fPlayerStates = playerStates
                .filterValues { insideSquareRadiusXZ(it.position, observer, r) }
            val fRobotStates = robotStates
                .filterValues { insideSquareRadiusXZ(it.position, observer, r) }
            val ownedRobots = pl.data.deployedRobots
                .map { (id, r) -> id to r.buildPrivateInfo() }
                .toMap()
            val fGroundItems = groundItems
                .filterValues { insideSquareRadiusXZ(it.position, observer, r) }
            return WorldStatePacket(
                relTimestamp = now - pl.connection.connectedSince,
                fPlayerStates, fRobotStates, ownedRobots, fGroundItems,
                triggeredObjects
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

    @Synchronized
    fun broadcastVfx(vfx: VisualEffect, origin: SerVector3) {
        val now: Long = System.currentTimeMillis()
        val maxDist = VisualEffect.MAX_BROADCAST_DIST
        for (player in this.players.values) {
            val pos = player.data.worlds.last().state.position
            if (!insideSquareRadiusXZ(pos, origin, maxDist)) { continue }
            player.connection.outgoing.send(Packet.serialize(
                PacketType.VISUAL_EFFECT, VisualEffectPacket(
                    relTimestamp = now - player.connection.connectedSince,
                    vfx
                )
            ))
        }
    }

    init {
        val ph = this.packetHandler

        ph.onDecodeError = { player, error ->
            player.connection.outgoing.send(Packet.serialize(
                PacketType.GENERIC_ERROR, GenericErrorPacket(error)
            ))
        }
        ph.onPacket(PacketType.REQUEST_CHUNK_CONTENTS) { r, pl ->
            if (r.chunks.size > MAX_NUM_REQUESTED_CHUNKS) {
                return@onPacket pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.TOO_MANY_CHUNKS_REQUESTED
                ))
            }
            val groundColor: GroundColorReader = this.static.groundColor
            val chunks = r.chunks.map {
                val data = this.static.world.chunks[it]
                it to SharedChunkData(
                    instances = data?.instances ?: listOf(),
                    groundColorTL = groundColor[it.chunkX, it.chunkZ],
                    groundColorTR = groundColor[it.chunkX + 1, it.chunkZ],
                    groundColorBL = groundColor[it.chunkX, it.chunkZ + 1],
                    groundColorBR = groundColor[it.chunkX + 1, it.chunkZ + 1],
                )
            }
            if (chunks.isEmpty()) { return@onPacket }
            pl.connection.outgoing.send(Packet.serialize(
                PacketType.CHUNK_CONTENTS, ChunkContentsPacket(chunks)
            ))
        }
        ph.onPacket(PacketType.REQUEST_WORLD_ENTER) { request, pl ->
            fun unknownWorld() = pl.connection.outgoing.send(Packet.serialize(
                PacketType.TAGGED_ERROR,
                TaggedErrorPacket.REQUESTED_WORLD_DOES_NOT_EXIST
            ))
            val world: World = this.registry[request.name]
                ?: return@onPacket unknownWorld()
            if (pl.data.worlds.size >= PlayerData.MAX_NUM_WORLDS) {
                return@onPacket pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.PLAYER_INSIDE_TOO_MANY_WORLDS
                ))
            }
            if (!pl.pushWorld(world.id, this.registry)) {
                return@onPacket unknownWorld()
            }
        }
        ph.onPacket(PacketType.REQUEST_WORLD_LEAVE) { _, pl ->
            pl.popWorld(this.registry)
        }

        ph.onPacket(PacketType.PLAYER_STATE) { pi, pl ->
            if (pl.username !in this.players.keys) { return@onPacket }
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
            val playerPosition = pl.data.worlds.last().state.position
            val robotPosition = Robot.allocatePosition(
                center = playerPosition, this,
                tileIsOccupied = { x, z ->
                    val notInArea = this.static.world
                        .maintenanceAreas.none { it.contains(x, z) }
                    notInArea || this.tileIsOccupied(x, z)
                }
            )
            if (robotPosition == null) {
                return@onPacket pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.NO_SPACE_FOR_ROBOT
                ))
            }
            val wasRemoved: Boolean = pl.data.inventory.tryRemove(d.item)
            if (!wasRemoved) {
                return@onPacket pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.REQUESTED_ROBOT_NOT_IN_INVENTORY
                ))
            }
            val robot = PlayerRobot(d.robotType, d.item, robotPosition)
            pl.data.deployedRobots[robot.id] = robot
            pl.connection.outgoing.send(Packet.serialize(
                PacketType.ROBOT_DEPLOYED,
                robot.id
            ))
        }
        fun getRobotOrError(robotId: Uuid, player: Player): PlayerRobot? {
            val robot: PlayerRobot? = player.data.deployedRobots[robotId]
            if (robot != null) { return robot }
            player.connection.outgoing.send(Packet.serialize(
                PacketType.TAGGED_ERROR, TaggedErrorPacket.NOT_ROBOT_OWNER
            ))
            return null
        }
        ph.onPacket(PacketType.DESTROY_ROBOT) { robotId, pl ->
            val robot = getRobotOrError(robotId, pl) ?: return@onPacket
            pl.data.deployedRobots.remove(robotId)
            val inv: Inventory = pl.data.inventory
            robot.collectContainedItems().forEach(inv::add)
            pl.connection.outgoing.send(Packet.serialize(
                PacketType.ROBOT_DESTROYED, robotId
            ))
        }
        ph.onPacket(PacketType.REPAIR_ROBOT) { robotId, pl ->
            val robot = getRobotOrError(robotId, pl) ?: return@onPacket
            val playerPos = pl.data.worlds.last().state.position
            val dist: Float = hypot(
                robot.position.x - playerPos.x, robot.position.z - playerPos.z
            )
            if (dist > MAX_ROBOT_REPAIR_DIST) {
                return@onPacket pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.ROBOT_TOO_FAR_AWAY_TO_REPAIR
                ))
            }
            val inArea = this.static.world
                .maintenanceAreas.any { it.contains(robot.tileX, robot.tileZ) }
            if (!inArea) {
                return@onPacket pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.ROBOT_NOT_IN_MAINTENANCE_AREA
                ))
            }
            robot.health = robot.type.maxHealth
            pl.connection.outgoing.send(Packet.serialize(
                PacketType.ROBOT_REPAIRED, robotId
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
            robot.resume()
        }
        ph.onPacket(PacketType.UNPAUSE_RESTART_ROBOT) { robotId, pl ->
            val robot = getRobotOrError(robotId, pl) ?: return@onPacket
            if (robot.status == RobotStatus.PAUSED) {
                robot.stop()
                robot.start()
            }
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

        ph.onPacket(PacketType.UP_CHAT_MESSAGE) { msg, pl ->
            if (msg.length > MAX_CHAT_MESSAGE_LENGTH) {
                pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.CHAT_MESSAGE_TOO_LONG
                ))
                return@onPacket
            }
            val now: Long = System.currentTimeMillis()
            val sinceLastMsg: Long = now - pl.lastChatMessage
            if (sinceLastMsg < CHAT_MESSAGE_COOLDOWN_MS) {
                pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.CHAT_MESSAGES_ON_COOLDOWN
                ))
                return@onPacket
            }
            pl.lastChatMessage = now
            val packet = Packet.serialize(
                PacketType.DOWN_CHAT_MESSAGE,
                DownChatMessagePacket(senderName = pl.username, message = msg)
            )
            for (player in this.players.values) {
                player.connection.outgoing.send(packet)
            }
        }
        ph.onPacket(PacketType.CHANGE_PLAYER_STYLE) { style, pl ->
            if (style.colors.size != PersonColorType.entries.size) {
                pl.connection.outgoing.send(Packet.serialize(
                    PacketType.TAGGED_ERROR,
                    TaggedErrorPacket.INVALID_COLOR_COUNT
                ))
                return@onPacket
            }
            pl.data.style = style
        }
        ph.onPacket(PacketType.REQUEST_DIALOGUE) { request, pl ->
            val dialogue: List<RemoteLocalization.Dialogue>
                = this.registry.services.localizations
                    .getDialogue(request.locale, request.selector)
            pl.connection.outgoing.send(Packet.serialize(
                PacketType.RECEIVE_DIALOGUE, dialogue
            ))
        }
    }

}