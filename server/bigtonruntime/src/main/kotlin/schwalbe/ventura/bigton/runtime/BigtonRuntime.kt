
package schwalbe.ventura.bigton.runtime

import java.nio.ByteBuffer

object BigtonRuntimeN {
    
    @JvmStatic external fun create(
        rawProgramBuf: ByteBuffer,
        rawProgramOffset: Int,
        rawProgramLength: Int,
        tickInstructionLimit: Long,
        memoryUsageLimit: Long,
        maxCallDepth: Int,
        maxTupleSize: Int
    ): Long
    @JvmStatic external fun free(runtimeHandle: Long)
    
    @JvmStatic external fun debugLoadedProgram(runtimeHandle: Long)
    
    @JvmStatic external fun addLogLine(
        runtimeHandle: Long, stringValueHandle: Long
    )
    @JvmStatic external fun getLogLineCount(runtimeHandle: Long): Int
    @JvmStatic external fun getLogLineAt(runtimeHandle: Long, i: Int): String
    @JvmStatic external fun clearLogLines(runtimeHandle: Long)
    
    @JvmStatic external fun getBacktraceLength(runtimeHandle: Long): Int
    @JvmStatic external fun getBacktraceName(
        runtimeHandle: Long, i: Int
    ): Int
    @JvmStatic external fun getBacktraceDeclFile(
        runtimeHandle: Long, i: Int
    ): Int
    @JvmStatic external fun getBacktraceDeclLine(
        runtimeHandle: Long, i: Int
    ): Int
    @JvmStatic external fun getBacktraceFromFile(
        runtimeHandle: Long, i: Int
    ): Int
    @JvmStatic external fun getBacktraceFromLine(
        runtimeHandle: Long, i: Int
    ): Int
    
    @JvmStatic external fun stackPush(runtimeHandle: Long, valueHandle: Long)
    @JvmStatic external fun stackPop(runtimeHandle: Long): Long
    @JvmStatic external fun getStackLength(runtimeHandle: Long): Int
    @JvmStatic external fun getStackAt(runtimeHandle: Long, i: Int): Long
    
    @JvmStatic external fun getNumConstStrings(runtimeHandle: Long): Int
    @JvmStatic external fun getConstString(runtimeHandle: Long, i: Int): String
    
    @JvmStatic external fun getCurrentFile(runtimeHandle: Long): Int
    @JvmStatic external fun getCurrentLine(runtimeHandle: Long): Int
    @JvmStatic external fun getUsedMemory(runtimeHandle: Long): Long
    
    @JvmStatic external fun getError(runtimeHandle: Long): Int
    @JvmStatic external fun setError(runtimeHandle: Long, error: Int)
    
    @JvmStatic external fun getAwaitingBuiltinId(runtimeHandle: Long): Int
    
    @JvmStatic external fun startTick(runtimeHandle: Long)
    @JvmStatic external fun executeBatch(runtimeHandle: Long): Int
    
}

class BigtonRuntime(
    program: ByteBuffer,
    tickInstructionLimit: Long,
    memoryUsageLimit: Long,
    maxCallDepth: Int,
    maxTupleSize: Int
) : AutoCloseable {
    
    companion object;
    
    data class TraceEntry(
        val name: BigtonConstStr,
        val declFile: BigtonConstStr, val declLine: Int,
        val fromFile: BigtonConstStr, val fromLine: Int
    )
    
    
    val handle: Long = BigtonRuntimeN.create(
        program, program.position(), program.remaining(),
        tickInstructionLimit, memoryUsageLimit,
        maxCallDepth, maxTupleSize
    )
    
    override fun close() = BigtonRuntimeN.free(this.handle)

}

fun BigtonRuntime.debugLoadedProgram()
    = BigtonRuntimeN.debugLoadedProgram(this.handle)

fun BigtonRuntime.logLine(line: String) {
    BigtonString.fromValue(line, this).use {
        BigtonRuntimeN.addLogLine(this.handle, it.handle)
    }
}

fun BigtonRuntime.drainLogLines(): List<String> {
    val lineCount: Int = BigtonRuntimeN.getLogLineCount(this.handle)
    val lines: List<String> = (0..<lineCount)
        .map { i -> BigtonRuntimeN.getLogLineAt(this.handle, i) }
    BigtonRuntimeN.clearLogLines(this.handle)
    return lines
}

fun BigtonRuntime.collectBacktrace(): List<BigtonRuntime.TraceEntry> {
    val traceCount: Int = BigtonRuntimeN.getBacktraceLength(this.handle)
    val h: Long = this.handle
    return (0..<traceCount).map { i -> BigtonRuntime.TraceEntry(
        name     = BigtonConstStr(BigtonRuntimeN.getBacktraceName    (h, i)),
        declFile = BigtonConstStr(BigtonRuntimeN.getBacktraceDeclFile(h, i)),
        declLine =                BigtonRuntimeN.getBacktraceDeclLine(h, i),
        fromFile = BigtonConstStr(BigtonRuntimeN.getBacktraceFromFile(h, i)),
        fromLine =                BigtonRuntimeN.getBacktraceFromLine(h, i)
    ) }
}

fun BigtonRuntime.pushStack(value: BigtonValue)
    = BigtonRuntimeN.stackPush(this.handle, value.handle)

fun BigtonRuntime.popStack(): BigtonValue? {
    val popped: Long = BigtonRuntimeN.stackPop(this.handle)
    if (popped == 0L) { return null }
    return BigtonValueN.wrapHandle(popped)
}

fun BigtonRuntime.collectStack(): List<BigtonValue> {
    val stackLength: Int = BigtonRuntimeN.getStackLength(this.handle)
    return (0..<stackLength).map { i ->
        val valueHandle: Long = BigtonRuntimeN.getStackAt(this.handle, i)
        BigtonValueN.wrapHandle(valueHandle)
    }
}

val BigtonRuntime.numConstStrings: Int
    get() = BigtonRuntimeN.getNumConstStrings(this.handle)

fun BigtonRuntime.getConstStr(s: BigtonConstStr): String {
    require(s.handle >= 0 && s.handle < this.numConstStrings)
    return BigtonRuntimeN.getConstString(this.handle, s.handle)
}
    
val BigtonRuntime.currentFile: BigtonConstStr
    get() = BigtonConstStr(BigtonRuntimeN.getCurrentFile(this.handle))
    
val BigtonRuntime.currentLine: Int
    get() = BigtonRuntimeN.getCurrentLine(this.handle)
    
val BigtonRuntime.usedMemory: Long
    get() = BigtonRuntimeN.getUsedMemory(this.handle)
    
var BigtonRuntime.error: BigtonRuntimeError
    get() {
        return BigtonRuntimeError.allTypes
            .getOrNull(BigtonRuntimeN.getError(this.handle))
            ?: BigtonRuntimeError.NONE
    }
    set(value) {
        BigtonRuntimeN.setError(this.handle, value.ordinal)
    }
    
fun BigtonRuntime.startTick()
    = BigtonRuntimeN.startTick(this.handle)

fun BigtonRuntime.executeBatch(): BigtonExecStatus {
    val status: Int = BigtonRuntimeN.executeBatch(this.handle)
    return when (status) {
        BigtonExecStatusType.CONTINUE -> BigtonExecStatus.Continue
        BigtonExecStatusType.EXEC_BUILTIN_FUN -> {
            val id: Int = BigtonRuntimeN.getAwaitingBuiltinId(this.handle)
            BigtonExecStatus.ExecBuiltinFun(id)
        }
        BigtonExecStatusType.AWAIT_TICK -> BigtonExecStatus.AwaitTick
        BigtonExecStatusType.COMPLETE -> BigtonExecStatus.Complete
        BigtonExecStatusType.ERROR -> BigtonExecStatus.Error(this.error)
        else -> throw IllegalStateException(
            "C runtime returned invalid execution status"
        )
    }
}