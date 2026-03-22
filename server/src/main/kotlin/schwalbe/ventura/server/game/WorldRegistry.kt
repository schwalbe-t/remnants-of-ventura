
package schwalbe.ventura.server.game

import schwalbe.ventura.data.WorldInstanceMode
import schwalbe.ventura.server.Workers
import schwalbe.ventura.server.persistence.serialize
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.uuid.Uuid

class WorldRegistry(
    val workers: Workers,
    val baseWorldName: String
) {

    sealed interface Entry {
        fun getInstanceId(name: String, registry: WorldRegistry): Uuid
        fun update(name: String, registry: WorldRegistry) {}
    }

    class ConstantEntry(val staticData: StaticWorldData) : Entry {
        var currentInstanceId: Uuid? = null

        override fun getInstanceId(
            name: String, registry: WorldRegistry
        ): Uuid {
            val existingId: Uuid? = this.currentInstanceId
            if (existingId != null) { return existingId }
            val newId: Uuid = Uuid.fromLongs(0L, name.hashCode().toLong())
            registry.createWorldInstance(name, newId, this.staticData)
            this.currentInstanceId = newId
            return newId
        }

        override fun update(name: String, registry: WorldRegistry) {
            this.getInstanceId(name, registry)
        }
    }

    class TemporaryEntry(val staticData: StaticWorldData) : Entry {
        class Session(val id: Uuid, var time: Long)

        companion object {
            const val OPEN_DURATION_MS: Long = 5 * 60_000
            const val CLOSE_IDLE_DURATION_MS: Long = 5 * 60_000
        }

        var open: Session? = null
        val closed: MutableList<Session> = mutableListOf()

        override fun getInstanceId(
            name: String, registry: WorldRegistry
        ): Uuid {
            val open: Session? = this.open
            if (open != null) { return open.id }
            val newId = registry.createWorldInstance(
                name, Uuid.random(), this.staticData
            )
            this.open = Session(newId, System.currentTimeMillis())
            return newId
        }

        override fun update(name: String, registry: WorldRegistry) {
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

    private val worldsById: MutableMap<Uuid, World> = mutableMapOf()
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
        this.updateEntries()
    }

    @Synchronized
    private fun createWorldInstance(
        name: String, id: Uuid, data: StaticWorldData
    ): Uuid {
        check(id !in this.worldsById.keys) {
            "World ID collision: Attempt to register $id more than once"
        }
        val world = World(this, name, id, data)
        this.worldsById[id] = world
        println("Created ${data.world.instanceMode} world instance $id for '$name'")
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
    fun remove(worldId: Uuid) {
        val removed: World = this.worldsById.remove(worldId) ?: return
        removed.handleWorldClosing()
        println("Closed world $worldId")
    }

    @Synchronized
    operator fun get(worldId: Uuid): World?
        = this.worldsById[worldId]

    @Synchronized
    operator fun get(worldName: String): World?
        = this.entryIdsByName[worldName]
            ?.getInstanceId(worldName, this)
            ?.let { id -> this[id] }

    @Synchronized
    private fun updateEntries() {
        for ((name, entry) in this.entryIdsByName) {
            entry.update(name, this)
        }
    }

    fun updateAll() {
        val worlds: List<World>
        synchronized(this) {
            this.updateEntries()
            worlds = this.worldsById.values.toList()
        }
        for (world in worlds) {
            this.updatePool.submit {
                world.update()
            }
        }
    }

    fun handlePlayerDisconnect(player: Player) {
        this.workers.playerWriter.add(player.serialize())
        synchronized(this) {
            for (world in this.worldsById.values) {
                world.handlePlayerDisconnect(player)
            }
        }
    }

}