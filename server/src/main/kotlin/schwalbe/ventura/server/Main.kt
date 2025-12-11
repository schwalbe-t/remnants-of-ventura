
package schwalbe.ventura.server

// import schwalbe.ventura.net.*
// import schwalbe.ventura.server.database.initDatabase
// import kotlinx.coroutines.*
// import kotlin.concurrent.thread
// import kotlin.time.Duration
// import kotlin.time.Duration.Companion.milliseconds
// import kotlin.time.Duration.Companion.seconds
// import kotlin.time.Duration.Companion.minutes
// import kotlinx.datetime.*
// import kotlin.system.exitProcess


// const val DEFAULT_KEYSTORE_PATH: String = "dev-keystore.p12"
// const val DEFAULT_KEYSTORE_ALIAS: String = "ventura"
// const val DEFAULT_KEYSTORE_PASS: String = "labubu"
// const val DEFAULT_PORT: Int = 8443

// fun getKeyStorePath(): String = System.getenv("VENTURA_KEYSTORE_PATH")
//     ?: DEFAULT_KEYSTORE_PATH

// fun getKeyStoreAlias(): String = System.getenv("VENTURA_KEYSTORE_ALIAS")
//     ?: DEFAULT_KEYSTORE_ALIAS

// fun getKeyStorePass(): String = System.getenv("VENTURA_KEYSTORE_PASS")
//     ?: DEFAULT_KEYSTORE_PASS

// fun getPort(): Int = System.getenv("VENTURA_PORT")?.toInt()
//     ?: DEFAULT_PORT

// private fun scheduled(interval: kotlin.time.Duration, f: () -> Unit) {
//     thread {
//         while (true) {
//             val startTime = Clock.System.now()
//             f()
//             val endTime = Clock.System.now()
//             val waitTime: Duration = interval - (endTime - startTime)
//             if (waitTime.isPositive()) {
//                 Thread.sleep(waitTime.inWholeMilliseconds)
//             }
//         }
//     }
// }

// class TestWorld(reg: WorldRegistry) : World(reg) {}

// fun main() {
//     initDatabase()

//     val playerWriter = PlayerWriter()
//     val createBaseWorld = { reg: WorldRegistry -> TestWorld(reg) }
//     val worlds = WorldRegistry(playerWriter, createBaseWorld)

//     val port: Int = getPort()
//     val createPlayerData = { PlayerData(ArrayDeque()) }
//     val server = Server(
//         getKeyStorePath(),
//         getKeyStoreAlias(),
//         getKeyStorePass(),
//         port,
//         worlds,
//         createPlayerData
//     )
//     println("Listening on port $port")
    
//     ServerNetwork.registerServer()

//     scheduled(interval = ServerNetwork.SERVER_REPORT_INTERVAL) {
//         if (!ServerNetwork.reportServerOnline()) {
//             server.stop(
//                 gracePeriodMillis = 5000,
//                 timeoutMillis = 5000
//             )
//             exitProcess(1)
//         }
//     }

//     scheduled(interval = 10.minutes) {
//         Session.deleteAllExpired()
//         ServerNetwork.deleteAllExpired()
//     }

//     CoroutineScope(Dispatchers.IO).launch {
//         playerWriter.writePlayers()
//     }

//     scheduled(interval = 50.milliseconds) {
//         worlds.updateAll()
//     }

//     scheduled(interval = 100.milliseconds) {
//         server.updateUnauthorized()
//     }
// }

import schwalbe.ventura.bigton.*
import schwalbe.ventura.bigton.runtime.*

fun main() {
    val program = BigtonProgram(
        functions = mapOf(
            "fib" to listOf(
                // fun fib(n) {
                BigtonInstr(BigtonInstrType.STORE_NEW_VARIABLE, "n"),
                // if (n <= 1) {
                BigtonInstr(BigtonInstrType.LOAD_VARIABLE, "n"),
                BigtonInstr(BigtonInstrType.LOAD_VALUE, BigtonInt(1)),
                BigtonInstr(BigtonInstrType.LESS_THAN_EQUAL),
                BigtonInstr(BigtonInstrType.IF, Pair(listOf<BigtonInstr>(
                    // return n
                    BigtonInstr(BigtonInstrType.LOAD_VARIABLE, "n"),
                    BigtonInstr(BigtonInstrType.RETURN)
                ), null)),
                // return fib(n - 1) + fib(n - 2)
                BigtonInstr(BigtonInstrType.LOAD_VARIABLE, "n"),
                BigtonInstr(BigtonInstrType.LOAD_VALUE, BigtonInt(1)),
                BigtonInstr(BigtonInstrType.SUBTRACT),
                BigtonInstr(BigtonInstrType.CALL, "fib"),
                BigtonInstr(BigtonInstrType.LOAD_VARIABLE, "n"),
                BigtonInstr(BigtonInstrType.LOAD_VALUE, BigtonInt(2)),
                BigtonInstr(BigtonInstrType.SUBTRACT),
                BigtonInstr(BigtonInstrType.CALL, "fib"),
                BigtonInstr(BigtonInstrType.ADD),
                BigtonInstr(BigtonInstrType.RETURN)
            )
        ),
        global = listOf(
            // var n = 0
            BigtonInstr(BigtonInstrType.LOAD_VALUE, BigtonInt(0)),
            BigtonInstr(BigtonInstrType.STORE_NEW_VARIABLE, "n"),
            // while (n < 20) {
            BigtonInstr(BigtonInstrType.LOOP, listOf<BigtonInstr>(
                BigtonInstr(BigtonInstrType.LOAD_VARIABLE, "n"),
                BigtonInstr(BigtonInstrType.LOAD_VALUE, BigtonInt(20)),
                BigtonInstr(BigtonInstrType.LESS_THAN),
                BigtonInstr(BigtonInstrType.NOT),
                BigtonInstr(BigtonInstrType.IF, Pair(listOf<BigtonInstr>(
                    BigtonInstr(BigtonInstrType.BREAK),
                ), null)),
                // say(fib(n))
                BigtonInstr(BigtonInstrType.LOAD_VARIABLE, "n"),
                BigtonInstr(BigtonInstrType.CALL, "fib"),
                BigtonInstr(BigtonInstrType.CALL, "say"),
                // n = n + 1
                BigtonInstr(BigtonInstrType.LOAD_VARIABLE, "n"),
                BigtonInstr(BigtonInstrType.LOAD_VALUE, BigtonInt(1)),
                BigtonInstr(BigtonInstrType.ADD),
                BigtonInstr(BigtonInstrType.STORE_EXISTING_VARIABLE, "n")
            ))
        )
    )
    val runtime = BigtonRuntime(
        program,
        modules = listOf(getStandardModule()),
        memorySize = 0,
        tickInstructionLimit = 500_000
    )   
    try {
        runtime.executeTick()
    } catch (e: BigtonException) {
        println(e.message)
    }
    println(runtime.logs.joinToString("\n"))
}