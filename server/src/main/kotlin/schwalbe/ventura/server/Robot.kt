
package schwalbe.ventura.server

import schwalbe.ventura.bigton.runtime.*
import schwalbe.ventura.bigton.*
import schwalbe.ventura.data.ItemType
import schwalbe.ventura.net.SerVector3
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.nio.ByteBuffer
import java.nio.ByteOrder

import kotlin.math.roundToLong

val Int.kb: Long
    get() = this * 1024L

val Double.kb: Long
    get() = (this * 1024.0).roundToLong()

enum class RobotType(
    val itemType: ItemType,
    val numAttachments: Int,
    val maxHealth: Float = 100f
) {
    SCOUT(
        itemType = ItemType.KENDAL_DYNAMICS_SCOUT,
        numAttachments = 4
    )
}

class RobotStats(
    val processor: ProcessorInfo,
    val totalModules: List<BigtonModule<World>>,
    val totalMemoryLimit: Long
)

private fun findAttachedProcessor(
    attachments: Array<ItemType?>, logs: MutableList<String>
): ProcessorInfo? {
    var found: ProcessorInfo? = null
    for (item in attachments) {
        val info = PROCESSOR_INFO[item ?: continue] ?: continue
        if (found == null) {
            found = info
            continue
        }
        logs.add(
            "ERROR: Robot has multiple processors attached! Only one " +
            "processor may be attached at a time."
        )
        return null
    }
    if (found != null) { return found }
    logs.add(
        "ERROR: Robot does not have a processor attached! A processor must " +
        "be attached before the robot can be started."
    )
    return null
}

private fun computeRobotStats(
    attachments: Array<ItemType?>, logs: MutableList<String>
): RobotStats? {
    val processor: ProcessorInfo = findAttachedProcessor(attachments, logs)
        ?: return null
    val totalModules: MutableList<BigtonModule<World>>
        = processor.features.modules.toMutableList()
    var totalMemoryLimit: Long = processor.stats.baseMemory
    for (item in attachments) {
        if (item in PROCESSOR_INFO) { continue }
        val info = ATTACHMENT_INFO[item ?: continue] ?: continue
        if (item !in processor.features.supportedAttachments) {
            logs.add(
                "WARNING: The robot will not be able to make use of the " +
                "attachment '${item.name}', as it is not supported by the " +
                "attached processor."
            )
            continue
        }
        totalModules.addAll(info.addedModules)
        totalMemoryLimit += info.addedMemory
    }
    return RobotStats(processor, totalModules, totalMemoryLimit)
}

enum class RobotState {
    STOPPED,
    RUNNING,
    PAUSED,
    ERROR
}

@Serializable
class Robot(val type: RobotType) {

    var name: String = "Unnamed Robot"
    val position = SerVector3(0f, 0f, 0f)
    var health: Float = this.type.maxHealth

    var state: RobotState = RobotState.STOPPED
        private set

    @Transient
    private var stats: RobotStats? = null
    @Transient
    private var compileTask: CompilationTask? = null
    @Transient
    private var runtime: BigtonRuntime? = null

    val logs: MutableList<String> = mutableListOf()
    val attachments: Array<ItemType?> = Array(this.type.numAttachments) { null }

    fun start() {
        when (this.state) {
            RobotState.RUNNING, RobotState.PAUSED -> {}
            RobotState.STOPPED, RobotState.ERROR -> {
                this.logs.clear()
                this.stats = null
                this.compileTask = null
                this.runtime = null
            }
        }
        this.state = RobotState.RUNNING
    }

    fun pause() {
        when (this.state) {
            RobotState.RUNNING -> {
                this.state = RobotState.PAUSED
            }
            RobotState.PAUSED, RobotState.STOPPED, RobotState.ERROR -> {}
        }
    }

    fun stop() {
        when (this.state) {
            RobotState.RUNNING, RobotState.PAUSED -> {
                this.state = RobotState.STOPPED
            }
            RobotState.STOPPED, RobotState.ERROR -> {}
        }
    }

    private fun logError(
        type: BigtonErrorType, atLine: Int, inFile: String,
        runtime: BigtonRuntime?
    ) {
        this.logs.add("ERROR: ${type.message} [${type.id}]")
        if (runtime == null) {
            this.logs.add("    at line $atLine, file '$inFile'")
            return
        }
        this.logs.add("Backtrace (latest call first):")
        var currLine: Int = atLine
        var currFile: String = inFile
        for (traceEntry in runtime.collectBacktrace().reversed()) {
            val name: String = runtime.getConstStr(traceEntry.name)
            this.logs.add("    at '$name' (line $currLine, file '$currFile')")
            currLine = traceEntry.fromLine
            currFile = runtime.getConstStr(traceEntry.fromFile)
        }
    }

    private fun updateCompilation(compilationQueue: CompilationQueue) {
        var stats: RobotStats? = this.stats
        if (stats == null) {
            stats = computeRobotStats(this.attachments, this.logs)
        }
        if (stats == null) {
            this.state = RobotState.ERROR
            return
        }
        this.stats = stats
        val task: CompilationTask? = this.compileTask
        if (task == null) {
            val newTask = CompilationTask(
                sources = listOf(), // TODO! actual sources
                features = stats.processor.features.features,
                modules = stats.totalModules,
                BIGTON_MODULES.functions
            )
            compilationQueue.add(newTask)
            this.compileTask = newTask
            return
        }
        when (val status = task.status) {
            is CompilationTask.Waiting, is CompilationTask.InProgress -> {}
            is CompilationTask.Success -> {
                val programBuffer: ByteBuffer = ByteBuffer
                    .allocateDirect(status.binary.size)
                    .order(ByteOrder.nativeOrder())
                    .put(status.binary).flip()
                val procStats = stats.processor.stats
                this.runtime = BigtonRuntime(
                    programBuffer,
                    tickInstructionLimit = procStats.instructionLimit,
                    memoryUsageLimit = stats.totalMemoryLimit,
                    maxCallDepth = procStats.maxCallDepth,
                    maxTupleSize = procStats.maxTupleSize
                )
            }
            is CompilationTask.Failed -> {
                val src: BigtonSource = status.error.source
                this.logError(
                    status.error.error, src.line, src.file, runtime = null
                )
                this.state = RobotState.ERROR
            }
        }
    }

    fun update(world: World) {
        if (this.state != RobotState.RUNNING) {
            this.compileTask = null
            this.runtime = null
            return
        }
        val runtime: BigtonRuntime? = this.runtime
        if (runtime == null) {
            this.updateCompilation(world.registry.workers.compilationQueue)
            return
        }
        runtime.startTick()
        while (true) {
            val execStatus = runtime.executeBatch()
            this.logs.addAll(runtime.drainLogLines())
            when (execStatus) {
                is BigtonExecStatus.Continue -> continue
                is BigtonExecStatus.ExecBuiltinFun -> {
                    val f: BuiltinFunctionInfo<World>
                        = BIGTON_MODULES.functions.functions[execStatus.id]
                    f.impl(runtime, world)
                }
                is BigtonExecStatus.AwaitTick -> break
                is BigtonExecStatus.Complete -> {
                    this.state = RobotState.STOPPED
                    break
                }
                is BigtonExecStatus.Error -> {
                    this.logError(
                        BigtonErrorType.fromRuntimeError(execStatus.error),
                        runtime.currentLine,
                        runtime.getConstStr(runtime.currentFile),
                        runtime
                    )
                    this.state = RobotState.ERROR
                    break
                }
            }
        }
    }

    fun getFracHealth(): Float = this.health / this.type.maxHealth

    fun getFracMemUsage(): Float {
        val stats: RobotStats = this.stats ?: return 0f
        val runtime: BigtonRuntime = this.runtime ?: return 0f
        val memLimit: Long = stats.totalMemoryLimit
        return (runtime.usedMemory.toDouble() / memLimit.toDouble()).toFloat()
    }

    fun getFracCpuUsage(): Float {
        val stats: RobotStats = this.stats ?: return 0f
        val runtime: BigtonRuntime = this.runtime ?: return 0f
        val instrLimit: Long = stats.processor.stats.instructionLimit
        return (runtime.usedInstrCost.toDouble() / instrLimit.toDouble())
            .toFloat()
    }

}
