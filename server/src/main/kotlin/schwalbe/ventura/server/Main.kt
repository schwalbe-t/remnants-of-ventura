
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
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Paths

fun main() {
    loadBigtonRuntime("bigtonruntime", "bigtonruntime")
    
    
    val utilsSrc = """
    
    fun lnFib(n) {
        var a = 0
        var b = 1
        var i = 0
        while i < n {
            var c = a + b
            a = b
            b = c
            i = i + 1
        }
        return a
    }
    
    fun recFib(n) {
        if n <= 1 { return n }
        return recFib(n - 1) + recFib(n - 2)
    }
    
    """.trimIndent()
    
    val mainSrc = """
    
    var values = []
    push(values, 5)
    push(values, 10)
    insert(values, 0, 69)
    print(values)
    print(pop(values))
    print(pop(values))
    print(values)
    insert(values, 1, 420)
    print(values)
    
    """.trimIndent()
    
    val files = listOf(
        BigtonSourceFile("utils", utilsSrc),
        BigtonSourceFile("main", mainSrc)
    )
    val features = setOf(
        BigtonFeature.FPU_MODULE,
        BigtonFeature.CUSTOM_FUNCTIONS,
        BigtonFeature.DYNAMIC_MEMORY
    )
    val modules = listOf(
        BigtonModules.standard,
        BigtonModules.floatingPoint
    )
    val programBytes: ByteArray
    try {
        programBytes = compileSources(files, features, modules)
    } catch (e: BigtonException) {
        println(e.message)
        return
    }
    
    // val outPath = "bigtonruntime/test.bigtonm"
    // File(outPath).writeBytes(programBytes)
    // println("Wrote compilation output to '$outPath'")
    // return
    
    val rawProgram: ByteBuffer = ByteBuffer.allocateDirect(programBytes.size)
        .order(ByteOrder.nativeOrder())
        .put(programBytes)
        .flip()
        
    val runtime = BigtonRuntime(
        program = rawProgram,
        tickInstructionLimit = 9999,
        memoryUsageLimit = 1024 * 16,
        maxCallDepth = 128,
        maxTupleSize = 8
    )
    check(runtime.error == BigtonRuntimeError.NONE) { runtime.error }
    
    runtime.debugLoadedProgram()
    
    runtime.startTick()
    try {
        while (true) {
            val status = runtime.executeBatch()
            when (status) {
                is BigtonExecStatus.Continue -> {}
                is BigtonExecStatus.ExecBuiltinFun -> {
                    val f: BuiltinFunctionInfo
                        = BigtonModules.functions.functions[status.id]
                    f.impl(runtime)
                }
                is BigtonExecStatus.AwaitTick -> {
                    runtime.startTick()
                }
                is BigtonExecStatus.Complete -> {
                    println("Program execution finished")
                    break
                }
                is BigtonExecStatus.Error -> {
                    throw BigtonException(
                        BigtonErrorType.fromRuntimeError(status.error),
                        BigtonSource(
                            line = runtime.currentLine,
                            file = runtime.getConstStr(runtime.currentFile)
                        )
                    )
                }
            }
        }
    } catch (e: BigtonException) {
        println(e.message)
    }
    val loggedLines: List<String> = runtime.drainLogLines()
    println("Last ${loggedLines.size} logged line(s):")
    loggedLines.forEach(::println)
    
    runtime.close()
}