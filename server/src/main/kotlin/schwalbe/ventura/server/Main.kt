
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
import java.io.DataOutputStream;
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Paths

fun main() {
    BigtonRuntime.loadLibrary("bigtonruntime", "bigtonruntime")
    
    
    val utilsSrc = """
    
    fun fib(n) {
        if n <= 1 { return n }
        return fib(n - 1) + fib(n - 2)
    }
    
    """.trimIndent()
    
    val mainSrc = """
            
    var n = 0
    while n <= 11 {
        print(fib(n))
        n = n + 1
    }
    
    """.trimIndent()
    
    val files = listOf(
        BigtonSourceFile("utils", utilsSrc),
        BigtonSourceFile("main", mainSrc)
    )
    val features = setOf(
        BigtonFeature.RAM_MODULE,
        BigtonFeature.CUSTOM_FUNCTIONS
    )
    val modules = listOf(
        BigtonModules.standard
    )
    val programBytes: ByteArray
    try {
        programBytes = compileSources(files, features, modules)
    } catch (e: BigtonException) {
        println(e.message)
        return
    }
    
    
    val tickInstructionLimit: Long = 99999999
    val memoryUsageLimit: Long = 1024 * 16
    val maxCallDepth = 128
    val maxTupleSize = 8
    val settings: Long = BigtonRuntime.createSettings(
        tickInstructionLimit, memoryUsageLimit,
        maxCallDepth, maxTupleSize
    )
    
    val rawProgram: ByteBuffer = ByteBuffer.allocateDirect(programBytes.size)
        .order(ByteOrder.nativeOrder())
        .put(programBytes)
        .flip()
        
    val runtime: Long = BigtonRuntime.create(
        settings, rawProgram, rawProgram.position(), rawProgram.remaining()
    )
    check(runtime != 0L)
    check(!BigtonRuntime.hasError(runtime)) { BigtonRuntime.getError(runtime) }
    
    BigtonRuntime.debugLoadedProgram(runtime)
    
    BigtonRuntime.startTick(runtime)
    while (true) {
        val status: Int = BigtonRuntime.execBatch(runtime)
        when {
            BigtonRuntime.hasError(runtime) ||
            status == BigtonRuntime.Status.ERROR -> {
                val error: Int = BigtonRuntime.getError(runtime)
                println("Stopped with error code $error")
                break
            }
            status == BigtonRuntime.Status.CONTINUE -> {}
            status == BigtonRuntime.Status.EXEC_BUILTIN_FUN -> {
                val builtinId: Int
                    = BigtonRuntime.getAwaitingBuiltinFun(runtime)
                val f: BuiltinFunctionInfo
                    = BigtonModules.functions.functions[builtinId]
                f.impl(runtime)
            }
            status == BigtonRuntime.Status.AWAIT_TICK -> {
                println("Next tick")
                BigtonRuntime.startTick(runtime)
            }
            status == BigtonRuntime.Status.COMPLETE -> {
                println("Program execution finished")
                break
            }
        }
    }
    val currFile: String = BigtonRuntime.getConstString(
        runtime, BigtonRuntime.getCurrentFile(runtime)
    )
    val currLine: Int = BigtonRuntime.getCurrentLine(runtime)
    println("Current file: '$currFile'")
    println("Current line: $currLine")
    println("Stack:")
    val stackSize: Long = BigtonRuntime.getStackLength(runtime)
    for (i in 0..<stackSize) {
        val value: Long = BigtonRuntime.getStack(runtime, i)
        val str: String = when (BigtonValue.getType(value)) {
            BigtonValue.Type.NULL -> "null"
            BigtonValue.Type.INT -> BigtonValue.getInt(value).toString()
            BigtonValue.Type.FLOAT -> BigtonValue.getFloat(value).toString()
            BigtonValue.Type.TUPLE -> "<tuple>"
            BigtonValue.Type.ARRAY -> "<array>"
            BigtonValue.Type.OBJECT -> "<object>"
            else -> "<unknown>"
        }
        println(" - $i: $str")
        BigtonValue.free(value)
    }
    println("Logs:")
    for (lineI in 0..<BigtonRuntime.getLogLength(runtime)) {
        val line: String = BigtonRuntime.getLogString(runtime, lineI)
        println(line)
    }
    
    BigtonRuntime.free(runtime)
    BigtonRuntime.freeSettings(settings)
    
    // TODO! fun stuff
    
    // BigtonRuntime.free(runtime)
    
    // try {

    //     val utilsSrc = """
        
    //     fun range(i, max) {
    //         if i >= max { return null }
    //         return (i, range(i + 1, max))
    //     }

    //     """
    //     val mainSrc = """
        
    //     print(range(0, 4))
    //     print(range(0, 8))
        
    //     """

    //     val files = listOf(
    //         BigtonSourceFile("utils", utilsSrc),
    //         BigtonSourceFile("main", mainSrc)
    //     )
    //     val features = setOf(
    //         BigtonFeature.RAM_MODULE,
    //         BigtonFeature.CUSTOM_FUNCTIONS
    //     )
    //     val modules = listOf(
    //         bigtonStandardModule
    //     )
    //     val program: BigtonProgram = compileSources(files, features, modules)

    //     // println(program.displayInstr())

    //     val runtime = BigtonRuntime(
    //         program, modules,
    //         memorySize = 100,
    //         tickInstructionLimit = Long.MAX_VALUE,
    //         maxCallDepth = 100,
    //         maxTupleSize = 8
    //     )
    //     try {
    //         val startTime: Long = System.currentTimeMillis()
    //         runtime.executeTick()
    //         val endTime: Long = System.currentTimeMillis()
    //         println("Execution time: ${endTime - startTime}ms")
    //     } catch (e: BigtonException) {
    //         runtime.logStackTrace(e)
    //     }
    //     println(runtime.getLogString())

    // } catch (e: BigtonException) {
    //     println(e.message)
    // }
}