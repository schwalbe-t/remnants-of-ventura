
package schwalbe.ventura.server

import schwalbe.ventura.net.*
import schwalbe.ventura.server.database.initDatabase
import kotlinx.coroutines.*
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.*
import kotlin.system.exitProcess


const val DEFAULT_KEYSTORE_PATH: String = "dev-keystore.p12"
const val DEFAULT_KEYSTORE_ALIAS: String = "ventura"
const val DEFAULT_KEYSTORE_PASS: String = "labubu"
const val DEFAULT_PORT: Int = 8443

fun getKeyStorePath(): String = System.getenv("VENTURA_KEYSTORE_PATH")
    ?: DEFAULT_KEYSTORE_PATH

fun getKeyStoreAlias(): String = System.getenv("VENTURA_KEYSTORE_ALIAS")
    ?: DEFAULT_KEYSTORE_ALIAS

fun getKeyStorePass(): String = System.getenv("VENTURA_KEYSTORE_PASS")
    ?: DEFAULT_KEYSTORE_PASS

fun getPort(): Int = System.getenv("VENTURA_PORT")?.toInt()
    ?: DEFAULT_PORT

private fun scheduled(interval: kotlin.time.Duration, f: () -> Unit) {
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

class TestWorld(reg: WorldRegistry) : World(reg) {}

fun main() {
    initDatabase()

    val playerWriter = PlayerWriter()
    val createBaseWorld = { reg: WorldRegistry -> TestWorld(reg) }
    val worlds = WorldRegistry(playerWriter, createBaseWorld)

    val port: Int = getPort()
    val createPlayerData = { PlayerData(ArrayDeque()) }
    val server = Server(
        getKeyStorePath(),
        getKeyStoreAlias(),
        getKeyStorePass(),
        port,
        worlds,
        createPlayerData
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

    CoroutineScope(Dispatchers.IO).launch {
        playerWriter.writePlayers()
    }

    scheduled(interval = 50.milliseconds) {
        worlds.updateAll()
    }

    scheduled(interval = 100.milliseconds) {
        server.updateUnauthorized()
    }
}