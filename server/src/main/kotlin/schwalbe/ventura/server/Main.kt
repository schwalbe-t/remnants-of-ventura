
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
    
    val tickInstructionLimit: Long = 1024
    val memoryUsageLimit: Long = 1024 * 16
    val maxCallDepth = 128
    val maxTupleSize = 8
    val settings: Long = BigtonRuntime.createSettings(
        tickInstructionLimit, memoryUsageLimit,
        maxCallDepth, maxTupleSize
    )
    
    val p = BinaryWriter()
    // --- bigton_program_t header ---
    p.beginStruct()
    p.putInt(3)         // bigton_instr_idx_t numInstrs
    p.putInt(1)         // bigton_str_id_t numStrings
    p.putInt(0)         // bigton_shape_id_t numShapes
    p.putInt(0)         // bigton_slot_t numFunctions
    p.putInt(0)         // bigton_slot_t numBuiltinFunctions
    p.putInt(0)         // bigton_slot_t numGlobalVars
    p.putInt(0)         // uint32_t numShapeProps
    p.putLong(9)        // uint64_t numConstStringChars
    p.putInt(0)         // bigton_str_id_t unknownStrId
    p.putInt(0)         // bigton_instr_idx_t globalStart
    p.putInt(3)         // bigton_instr_idx_t globalLength
    p.endStruct()
    // --- bigton_instr_args_t instrArgs[header.numInstrs] ---
    p.putLong(5)        // [0] bigton_int_t loadInt
    p.putLong(10)       // [1] bigton_int_t loadInt
    p.putLong(0)        // [2] (no data)
    // --- bigton_const_string_t constStrings[header.numStrings] ---
    p.beginStruct()     // [0]
    p.putLong(0)        // uint64_t firstOffset
    p.putLong(9)        // uint64_t charLength
    p.endStruct()
    // --- bigton_shape_t shapes[header.numShapes] ---
    // --- bigton_function_t functions[header.numFunctions] ---
    // --- bigton_builtin_function_t builtinFunctions[header.numBuiltinFunctions] ---
    // --- bigton_shape_prop_t shapeProps[header.numShapeProps] ---
    // --- bigton_char_t constStringChars[header.numConstStringChars] ---
    p.putString("<unknown>")
    // --- bigton_instr_type_t instrTypes[header.numInstrs] ---
    p.putByte(4)        // BIGTONIR_LOAD_INT
    p.putByte(4)        // BIGTONIR_LOAD_INT
    p.putByte(15)       // BIGTONIR_ADD
    
    val programBytes: ByteArray = p.output.toByteArray()
    val rawProgram: ByteBuffer = ByteBuffer.allocateDirect(programBytes.size)
        .order(ByteOrder.nativeOrder())
        .put(programBytes)
        .flip()
        
    val runtime: Long = BigtonRuntime.create(
        settings, rawProgram, rawProgram.position(), rawProgram.remaining()
    )
    check(runtime != 0L)
    check(!BigtonRuntime.hasError(runtime)) { BigtonRuntime.getError(runtime) }
    
    BigtonRuntime.startTick(runtime)
    execLoop@while (true) {
        val status: Int = BigtonRuntime.execBatch(runtime)
        when {
            BigtonRuntime.hasError(runtime) ||
            status == BigtonRuntime.Status.ERROR -> {
                val error: Int = BigtonRuntime.getError(runtime)
                println("Stopped with error code $error")
            }
            status == BigtonRuntime.Status.CONTINUE -> {}
            status == BigtonRuntime.Status.EXEC_BUILTIN_FUN -> {
                val builtinId: Int
                    = BigtonRuntime.getAwaitingBuiltinFun(runtime)
                println("Execute builtin fun [${builtinId}]")
                error("not implemented")
            }
            status == BigtonRuntime.Status.AWAIT_TICK -> {
                println("Next tick")
                BigtonRuntime.startTick(runtime)
            }
            status == BigtonRuntime.Status.COMPLETE -> {
                println("Program execution finished")
                break@execLoop
            }
        }
    }
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
        print(" - $i: $str")
        BigtonValue.free(value)
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