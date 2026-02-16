
package schwalbe.ventura.server

import schwalbe.ventura.bigton.runtime.*
import schwalbe.ventura.bigton.*
import schwalbe.ventura.data.ItemType
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class RobotType(
    val itemType: ItemType,
    val numAttachments: Int
) {
    SCOUT(
        itemType = ItemType.KENDAL_DYNAMICS_SCOUT,
        numAttachments = 4
    )
}

enum class RobotState {
    STOPPED,
    ERROR,
    RUNNING
}

@Serializable
class Robot(val type: RobotType) {

    var state: RobotState = RobotState.STOPPED
        private set

    @Transient
    private var compileTask: CompilationTask? = null
    @Transient
    private var runtime: BigtonRuntime? = null

    val logs: MutableList<String> = mutableListOf()
    val attachments: Array<ItemType?> = Array(this.type.numAttachments) { null }

    fun start() {
        this.logs.clear()
        this.compileTask = null
        this.runtime = null
        this.state = RobotState.RUNNING
    }

    fun stop() {
        this.state = RobotState.STOPPED
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
        val task: CompilationTask? = this.compileTask
        if (task == null) {
            val newTask = CompilationTask(
                // TODO! actual values
                sources = listOf(),
                features = setOf(),
                modules = listOf(),
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
                this.runtime = BigtonRuntime(
                    programBuffer,
                    // TODO! actual values
                    tickInstructionLimit = 9999,
                    memoryUsageLimit = 9999,
                    maxCallDepth = 128,
                    maxTupleSize = 8
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

    val cpuUsagePercent: Float = (this.runtime?.usedInstrCost ?: 0L).toDouble() /

}
