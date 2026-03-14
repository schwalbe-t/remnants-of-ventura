
package schwalbe.ventura.server.game

import schwalbe.ventura.data.WorldInstanceMode
import schwalbe.ventura.server.Workers
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WorldRegistry(
    val workers: Workers,
    baseWorldData: StaticWorldData
) {

    sealed class Entry(val name: String?) {
        abstract val instance: World?
        abstract fun getInstance(registry: WorldRegistry, entryId: Long): World
    }

    class ConstantEntry(
        val staticData: StaticWorldData,
        name: String? = null
    ) : Entry(name) {
        override var instance: World? = null

        override fun getInstance(
            registry: WorldRegistry, entryId: Long
        ): World {
            val world: World = this.instance
                ?: World(registry, entryId, WorldInstanceData(this.staticData))
            this.instance = world
            return world
        }
    }


    private val entriesById: MutableMap<Long, Entry> = mutableMapOf()
    private var nextId: Long = 0
    private val entryIdsByName: MutableMap<String, Long> = mutableMapOf()

    private val updatePool: ExecutorService

    val baseWorld: World

    init {
        val nproc: Int = Runtime.getRuntime().availableProcessors()
        this.updatePool = Executors.newFixedThreadPool(nproc)
        this.baseWorld = this.add(baseWorldData)
    }

    @Synchronized
    private fun insertEntry(entry: Entry): Long {
        val id: Long = this.nextId
        this.nextId += 1
        this.entriesById[id] = entry
        entry.name?.let { name -> this.entryIdsByName[name] = id }
        return id
    }

    @Synchronized
    fun add(data: StaticWorldData, name: String? = null): World {
        val entry: Entry = when (data.world.instanceMode) {
            WorldInstanceMode.CONSTANT -> ConstantEntry(data, name)
            WorldInstanceMode.TEMPORARY -> TODO("not yet implemented")
        }
        val id: Long = this.insertEntry(entry)
        return entry.getInstance(this, id)
    }

    @Synchronized
    fun remove(worldId: Long) {
        val removed: Entry = this.entriesById.remove(worldId) ?: return
        this.entryIdsByName.remove(removed.name)
    }

    @Synchronized
    operator fun get(worldId: Long): World?
        = this.entriesById[worldId]?.getInstance(this, worldId)

    @Synchronized
    operator fun get(worldName: String): World?
        = this.entryIdsByName[worldName]?.let { id -> this[id] }

    fun updateAll() {
        val worlds: List<World>
        synchronized(this) {
            worlds = this.entriesById.values.mapNotNull { it.instance }
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
            for (entry in this.entriesById.values) {
                val world: World = entry.instance ?: continue
                world.handlePlayerDisconnect(player) 
            }
        }
    }

}