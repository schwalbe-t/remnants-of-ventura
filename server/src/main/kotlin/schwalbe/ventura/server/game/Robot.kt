
package schwalbe.ventura.server.game

import schwalbe.ventura.bigton.runtime.*
import schwalbe.ventura.bigton.*
import schwalbe.ventura.data.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import schwalbe.ventura.net.PrivateRobotInfo
import schwalbe.ventura.net.SerVector3
import schwalbe.ventura.net.SharedRobotInfo
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToLong
import kotlin.uuid.Uuid

val Int.kb: Long
    get() = this * 1024L

val Double.kb: Long
    get() = (this * 1024.0).roundToLong()

class RobotStats(
    val processor: ProcessorInfo,
    val totalModules: List<BigtonModule<World>>,
    val totalMemoryLimit: Long
)

private fun findAttachedProcessor(
    attachments: Array<Item?>, logs: MutableList<String>
): ProcessorInfo? {
    var found: ProcessorInfo? = null
    for (item in attachments.asSequence().filterNotNull()) {
        val info = PROCESSOR_INFO[item.type] ?: continue
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
    robotType: RobotType, attachments: Array<Item?>, logs: MutableList<String>
): RobotStats? {
    val totalModules = mutableListOf<BigtonModule<World>>()
    var totalMemoryLimit: Long = 0
    val robotInfo = ROBOT_TYPE_EXT[robotType] ?: RobotExtensions()
    totalModules.addAll(robotInfo.addedModules)
    totalMemoryLimit += robotInfo.addedMemory
    val processor: ProcessorInfo = findAttachedProcessor(attachments, logs)
        ?: return null
    totalModules.addAll(processor.features.modules)
    totalMemoryLimit += processor.stats.baseMemory
    for (item in attachments.asSequence().filterNotNull()) {
        if (item.type in PROCESSOR_INFO) { continue }
        val info = ATTACHMENT_EXT[item.type] ?: continue
        if (item.type !in processor.features.supportedAttachments) {
            logs.add(
                "WARNING: The robot will not be able to make use of the " +
                "attachment '${item.type.name}', as it is not supported by " +
                "the attached processor."
            )
            continue
        }
        totalModules.addAll(info.addedModules)
        totalMemoryLimit += info.addedMemory
    }
    return RobotStats(processor, totalModules, totalMemoryLimit)
}

@Serializable
class Robot(
    val type: RobotType,
    val item: Item,
    var position: SerVector3
) {

    var name: String = "Unnamed Robot"
    val id: Uuid = Uuid.random()
    var health: Float = this.type.maxHealth
    var rotation: Float = 0f

    var status: RobotStatus = RobotStatus.STOPPED
        private set

    @Transient
    private var stats: RobotStats? = null
    @Transient
    private var compileTask: CompilationTask? = null
    @Transient
    private var runtime: BigtonRuntime? = null

    val logs: MutableList<String> = mutableListOf()
    val attachments: Array<Item?> = Array(this.type.numAttachments) { null }
    var sourceFiles: List<String> = listOf()

    fun start() {
        when (this.status) {
            RobotStatus.RUNNING, RobotStatus.PAUSED -> {}
            RobotStatus.STOPPED, RobotStatus.ERROR -> {
                this.logs.clear()
                this.stats = null
                this.compileTask = null
                this.runtime = null
            }
        }
        this.status = RobotStatus.RUNNING
    }

    fun pause() {
        when (this.status) {
            RobotStatus.RUNNING -> {
                this.status = RobotStatus.PAUSED
            }
            RobotStatus.PAUSED, RobotStatus.STOPPED, RobotStatus.ERROR -> {}
        }
    }

    fun stop() {
        when (this.status) {
            RobotStatus.RUNNING, RobotStatus.PAUSED -> {
                this.status = RobotStatus.STOPPED
            }
            RobotStatus.STOPPED, RobotStatus.ERROR -> {}
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

    private fun updateCompilation(
        compilationQueue: CompilationQueue, sourceFiles: SourceFiles
    ) {
        var stats: RobotStats? = this.stats
        if (stats == null) {
            stats = computeRobotStats(this.type, this.attachments, this.logs)
        }
        if (stats == null) {
            this.status = RobotStatus.ERROR
            return
        }
        this.stats = stats
        val task: CompilationTask? = this.compileTask
        if (task == null) {
            val newTask = CompilationTask(
                sources = this.sourceFiles.map { path ->
                    BigtonSourceFile(path, sourceFiles.getContent(path))
                },
                features = stats.processor.features.features,
                modules = stats.totalModules,
                BIGTON_MODULES.functions
            )
            compilationQueue.add(newTask)
            this.compileTask = newTask
            return
        }
        when (val compStatus = task.status) {
            is CompilationTask.Waiting, is CompilationTask.InProgress -> {}
            is CompilationTask.Success -> {
                val programBuffer: ByteBuffer = ByteBuffer
                    .allocateDirect(compStatus.binary.size)
                    .order(ByteOrder.nativeOrder())
                    .put(compStatus.binary).flip()
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
                val src: BigtonSource = compStatus.error.source
                this.logError(
                    compStatus.error.error, src.line, src.file, runtime = null
                )
                this.status = RobotStatus.ERROR
            }
        }
    }

    fun update(world: World, owner: Player) {
        if (this.status != RobotStatus.RUNNING) {
            this.compileTask = null
            this.runtime = null
            return
        }
        val runtime: BigtonRuntime? = this.runtime
        if (runtime == null) {
            this.updateCompilation(
                world.registry.workers.compilationQueue, owner.data.sourceFiles
            )
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
                    this.status = RobotStatus.STOPPED
                    break
                }
                is BigtonExecStatus.Error -> {
                    this.logError(
                        BigtonErrorType.fromRuntimeError(execStatus.error),
                        runtime.currentLine,
                        runtime.getConstStr(runtime.currentFile),
                        runtime
                    )
                    this.status = RobotStatus.ERROR
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

    fun buildLogString(): String = this.logs.joinToString(
        separator = "",
        transform = { it + "\n" }
    )

    fun buildSharedInfo() = SharedRobotInfo(
        this.name, this.item, this.status, this.position, this.rotation
    )

    fun buildPrivateInfo() = PrivateRobotInfo(
        this.attachments.toList(),
        this.sourceFiles,
        this.getFracHealth(),
        this.getFracMemUsage(),
        this.getFracCpuUsage()
    )

}
