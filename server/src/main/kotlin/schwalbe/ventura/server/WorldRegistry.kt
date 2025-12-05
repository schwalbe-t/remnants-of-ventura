
package schwalbe.ventura.server

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WorldRegistry {

    private val worlds: MutableMap<Int, World> = mutableMapOf()
    private var nextId: Int = 0
    private val freeWorldIds = ArrayDeque<Int>()

    private val updatePool: ExecutorService

    init {
        val nproc: Int = Runtime.getRuntime().availableProcessors()
        updatePool = Executors.newFixedThreadPool(nproc)
    }

    private fun allocateWorld(): Int {
        synchronized(this) {
            val free: Int? = this.freeWorldIds.removeFirstOrNull()
            if (free != null) { return free }
            val next: Int = this.nextId
            this.nextId += 1
            return next
        }
    }

    fun create(creator: (Int) -> World) {
        val id = this.allocateWorld()
        val world = creator(id)
        synchronized(this) {
            this.worlds[id] = world
        }
    }

    fun remove(worldId: Int) {
        synchronized(this) {
            val removed: World? = this.worlds.remove(worldId)
            if (removed == null) { return }
            this.freeWorldIds.add(worldId)
        }
    }

    fun get(worldId: Int): World? {
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

}