
package schwalbe.ventura.server

import schwalbe.ventura.bigton.runtime.loadBigtonRuntime
import schwalbe.ventura.net.*
import schwalbe.ventura.data.*
import schwalbe.ventura.server.game.CompilationQueue
import schwalbe.ventura.server.game.WorldRegistry
import schwalbe.ventura.server.persistence.*
import kotlinx.coroutines.*
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.*
import org.joml.Vector3f
import kotlin.system.exitProcess

fun getKeyStorePath(): String = System.getenv("VENTURA_KEYSTORE_PATH")
    ?: "dev-keystore.p12"

fun getKeyStoreAlias(): String = System.getenv("VENTURA_KEYSTORE_ALIAS")
    ?: "ventura"

fun getKeyStorePass(): String = System.getenv("VENTURA_KEYSTORE_PASS")
    ?: "labubu"

fun getPort(): Int = System.getenv("VENTURA_PORT")?.toInt()
    ?: 8443

fun getBigtonRuntimeDir(): String = System.getenv("VENTURA_BIGTON_LIB_DIR")
    ?: "bigtonruntime/build/libs/main"

private fun scheduled(interval: Duration, f: () -> Unit) {
    thread {
        while (true) {
            val startTime = Clock.System.now()
            f()
            val endTime = Clock.System.now()
            val waitTime: Duration = interval - (endTime - startTime)
            if (waitTime.isPositive()) {
                Thread.sleep(waitTime.inWholeMilliseconds)
            }
        }
    }
}

class Workers {
    val playerWriter = PlayerWriter()
    val compilationQueue = CompilationQueue()
}

fun main() {
    loadBigtonRuntime(getBigtonRuntimeDir(), name = "bigtonruntime")
    initDatabase()

    val baseWorldChunks: Map<ChunkRef, ChunkData>
        = (-20..20).flatMap { chunkX -> (-20..20).map { chunkZ ->
            ChunkRef(chunkX, chunkZ) to ChunkData(listOf(
                ObjectInstance(
                    ObjectType.ROCK,
                    Vector3f(
                        chunkX + Math.random().toFloat(), 0f,
                        chunkZ + Math.random().toFloat()
                    ).chunksToUnits().toSerVector3(),
                    SerVector3(
                        0f, (Math.random() * 2 * kotlin.math.PI).toFloat(), 0f
                    ),
                    SerVector3(1f, 1f, 1f)
                )
            ))
        } }
        .toMap()
    val baseWorld = WorldData(
        ConstWorldInfo(rendererConfig = RendererConfig.default),
        baseWorldChunks
    )

    val workers = Workers()
    val worlds = WorldRegistry(workers, baseWorld)

    val port: Int = getPort()
    val server = Server(
        getKeyStorePath(),
        getKeyStoreAlias(),
        getKeyStorePass(),
        port,
        worlds
    )
    println("Listening on port $port")

    ServerNetwork.registerServer()

    scheduled(interval = ServerNetwork.SERVER_REPORT_INTERVAL) {
        if (!ServerNetwork.reportServerOnline()) {
            server.stop(
                gracePeriodMillis = 5000,
                timeoutMillis = 5000
            )
            exitProcess(1)
        }
    }

    scheduled(interval = 10.minutes) {
        Session.deleteAllExpired()
        ServerNetwork.deleteAllExpired()
    }

    scheduled(interval = 50.milliseconds) {
        worlds.updateAll()
    }

    scheduled(interval = 100.milliseconds) {
        server.updateUnauthorized()
    }

    CoroutineScope(Dispatchers.IO).launch {
        workers.playerWriter.runWorker()
    }

    thread {
        workers.compilationQueue.runWorker()
    }
}
