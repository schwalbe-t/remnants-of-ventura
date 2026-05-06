
package schwalbe.ventura.server

import schwalbe.ventura.bigton.runtime.loadBigtonRuntime
import schwalbe.ventura.data.*
import schwalbe.ventura.server.game.CompilationQueue
import schwalbe.ventura.server.game.WorldRegistry
import schwalbe.ventura.server.game.StaticWorldData
import schwalbe.ventura.server.persistence.*
import schwalbe.ventura.utils.GroundColorReader
import kotlinx.coroutines.*
import kotlinx.datetime.*
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.system.exitProcess
import java.nio.file.Files
import java.nio.file.Path

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

fun getWorldFileDir(): String = System.getenv("VENTURA_WORLD_FILES_DIR")
    ?: "worlds"

fun getMainWorldName(): String = System.getenv("VENTURA_MAIN_WORLD_NAME")
    ?: "main"

fun getLocalizationsDir(): String = System.getenv("VENTURA_LOCALIZATIONS_DIR")
    ?: "localizations"

fun getFallbackLocale(): String = System.getenv("VENTURA_FALLBACK_LOCALE")
    ?: "en"

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

class Services(
    val localizations: Localizations
) {
    val playerWriter = PlayerWriter()
    val compilationQueue = CompilationQueue()
}



fun loadWorlds(dirPath: Path): Map<String, StaticWorldData> {
    val loaded = mutableMapOf<String, StaticWorldData>()
    for (worldFile in dirPath.listDirectoryEntries()) {
        if (worldFile.extension != "json") { continue }
        val rawData: String = Files.readString(worldFile)
        val serData: SerializedWorld = SerializedWorld.SERIALIZER
            .decodeFromString(rawData)
        val groundColor = GroundColorReader(
            worldFile.parent.resolve(serData.groundColor).toFile()
        )
        loaded[worldFile.nameWithoutExtension] = StaticWorldData(
            serData, groundColor
        )
    }
    return loaded
}

fun main() {
    loadBigtonRuntime(getBigtonRuntimeDir(), name = "bigtonruntime")
    initDatabase()

    val worldData: Map<String, StaticWorldData>
        = loadWorlds(Path.of(getWorldFileDir()))
    println("Loaded ${worldData.size} world(s)")

    val localizations = Localizations
        .readDirectory(Path.of(getLocalizationsDir()), getFallbackLocale())

    val services = Services(localizations)
    val worlds = WorldRegistry(services, getMainWorldName())
    worldData.forEach { (name, data) -> worlds.add(name, data) }

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
        services.playerWriter.runWorker()
    }

    thread {
        services.compilationQueue.runWorker()
    }
}
