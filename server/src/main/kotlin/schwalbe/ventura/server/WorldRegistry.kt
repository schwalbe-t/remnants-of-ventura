
package schwalbe.ventura.server

import schwalbe.ventura.data.WorldData
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WorldRegistry(
    val playerWriter: PlayerWriter,
    baseWorldData: WorldData
) {

    private val worlds: MutableMap<Long, World> = mutableMapOf()
    private var nextId: Long = 0

    private val updatePool: ExecutorService

    val baseWorld: World

    init {
        val nproc: Int = Runtime.getRuntime().availableProcessors()
        this.updatePool = Executors.newFixedThreadPool(nproc)
        this.baseWorld = this.createWorld(baseWorldData)
    }

    fun createWorld(data: WorldData): World {
        synchronized(this) {
            val id: Long = this.nextId
            this.nextId += 1
            val world = World(this, id, data)
            this.worlds[world.id] = world
            return world
        }
    }

    fun remove(worldId: Long) {
        synchronized(this) {
            this.worlds.remove(worldId)
        }
    }

    operator fun get(worldId: Long): World? {
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
                world.update()
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