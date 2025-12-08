
package schwalbe.ventura.server

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WorldRegistry(
    val playerWriter: PlayerWriter,
    baseWorldCreator: (Long) -> World
) {

    private val worlds: MutableMap<Long, World> = mutableMapOf()
    private var nextId: Long = 0

    private val updatePool: ExecutorService

    val baseWorldId: Long
    val baseWorld: World
        get() = this.get(this.baseWorldId)!!

    init {
        val nproc: Int = Runtime.getRuntime().availableProcessors()
        updatePool = Executors.newFixedThreadPool(nproc)
        baseWorldId = this.create(baseWorldCreator)
    }

    private fun allocateWorld(): Long {
        synchronized(this) {
            val next: Long = this.nextId
            this.nextId += 1
            return next
        }
    }

    fun create(creator: (Long) -> World): Long {
        val id = this.allocateWorld()
        val world = creator(id)
        synchronized(this) {
            this.worlds[id] = world
        }
        return id
    }

    fun remove(worldId: Long) {
        synchronized(this) {
            this.worlds.remove(worldId)
        }
    }

    fun get(worldId: Long): World? {
        synchronized(this) {
            return this.worlds.getOrDefault(worldId, null)
        }
    }

    fun updateAll() {
        val worlds: List<World>
        synchronized(this) {
            worlds = this.worlds.values.toList()
        }
        for (world in worlds) {
            this.updatePool.submit {
                world.update(this)
            }
        }
    }
    
    fun handlePlayerDisconnect(player: Player) {
        this.playerWriter.add(player)
        synchronized(this) {
            for (world in this.worlds.values) {
                world.handlePlayerDisconnect(player) 
            }
        }
    }

}