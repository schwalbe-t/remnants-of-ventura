
package schwalbe.ventura.server.game

import schwalbe.ventura.data.WorldInstanceMode
import schwalbe.ventura.server.Workers
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WorldRegistry(
    val workers: Workers,
    val baseWorldName: String
) {

    sealed interface Entry {
        fun getInstanceId(registry: WorldRegistry): Long
        fun update(registry: WorldRegistry) {}
    }

    class ConstantEntry(val staticData: StaticWorldData) : Entry {
        var currentInstanceId: Long? = null

        override fun getInstanceId(registry: WorldRegistry): Long {
            val instanceId: Long = this.currentInstanceId
                ?: registry.createWorldInstance(this.staticData)
            this.currentInstanceId = instanceId
            return instanceId
        }
    }

    class TemporaryEntry(val staticData: StaticWorldData) : Entry {
        class Session(val id: Long, var time: Long)

        companion object {
            const val OPEN_DURATION_MS: Long = 5 * 60_000
            const val CLOSE_IDLE_DURATION_MS: Long = 5 * 60_000
        }

        var open: Session? = null
        val closed: MutableList<Session> = mutableListOf()

        override fun getInstanceId(registry: WorldRegistry): Long {
            val open: Session? = this.open
            if (open != null) { return open.id }
            val newId: Long = registry.createWorldInstance(this.staticData)
            this.open = Session(newId, System.currentTimeMillis())
            return newId
        }

        override fun update(registry: WorldRegistry) {
            val now: Long = System.currentTimeMillis()
            val open: Session? = this.open
            if (open != null && open.time + OPEN_DURATION_MS < now) {
                this.closed.add(open)
                this.open = null
            }
            for (closed in this.closed.toList()) {
                val world: World? = registry[closed.id]
                if (world != null && world.hasPlayers()) {
                    closed.time = now
                    continue
                }
                if (now <= closed.time + CLOSE_IDLE_DURATION_MS) {
                    continue
                }
                this.closed.remove(closed)
                registry.remove(closed.id)
            }
        }
    }

    private val worldsById: MutableMap<Long, World> = mutableMapOf()
    private var nextId: Long = 0
    private val entryIdsByName: MutableMap<String, Entry> = mutableMapOf()

    private val updatePool: ExecutorService

    val baseWorld: World
        get() = this[this.baseWorldName]
            ?: throw IllegalArgumentException(
                "Provided base world name '${this.baseWorldName}' " +
                "does not refer to a registered world!"
            )

    init {
        val nproc: Int = Runtime.getRuntime().availableProcessors()
        this.updatePool = Executors.newFixedThreadPool(nproc)
    }

    @Synchronized
    private fun createWorldInstance(data: StaticWorldData): Long {
        val id: Long = this.nextId
        this.nextId += 1
        val world = World(this, id, WorldInstanceData(data))
        this.worldsById[id] = world
        println("Created world #$id (${data.world.instanceMode})")
        return id
    }

    @Synchronized
    fun add(name: String, data: StaticWorldData) {
        this.entryIdsByName[name] = when (data.world.instanceMode) {
            WorldInstanceMode.CONSTANT -> ConstantEntry(data)
            WorldInstanceMode.TEMPORARY -> TemporaryEntry(data)
        }
    }

    @Synchronized
    fun remove(worldId: Long) {
        val removed: World = this.worldsById.remove(worldId) ?: return
        removed.handleWorldClosing()
        println("Closed world #$worldId")
    }

    @Synchronized
    operator fun get(worldId: Long): World?
        = this.worldsById[worldId]

    @Synchronized
    operator fun get(worldName: String): World?
        = this.entryIdsByName[worldName]
            ?.getInstanceId(this)
            ?.let { id -> this[id] }

    fun updateAll() {
        val worlds: List<World>
        synchronized(this) {
            worlds = this.worldsById.values.toList()
        }
        for (world in worlds) {
            this.updatePool.submit {
                world.update()
            }
        }
    }

    fun handlePlayerDisconnect(player: Player) {
        this.workers.playerWriter.add(player)
        synchronized(this) {
            for (world in this.worldsById.values) {
                world.handlePlayerDisconnect(player)
            }
        }
    }

}