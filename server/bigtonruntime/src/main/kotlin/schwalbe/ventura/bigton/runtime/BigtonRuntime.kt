
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
        val name: String,
        val declFile: String, val declLine: Int,
        val fromFile: String, val fromLine: Int
    )
    
    
    val handle: Long = BigtonRuntimeN.create(
        program, program.position(), program.remaining(),
        tickInstructionLimit, memoryUsageLimit,
        maxCallDepth, maxTupleSize
    )
    
    override fun close() = BigtonRuntimeN.free(this.handle)

}

fun BigtonRuntime.debugLoadedProgram() {
    error("TODO!")
}

fun BigtonRuntime.logLine(line: String) {
    error("TODO!")
}

fun BigtonRuntime.collectLogs(): List<String> {
    error("TODO!")
}

fun BigtonRuntime.collectBacktrace(): List<BigtonRuntime.TraceEntry> {
    error("TODO!")
}

fun BigtonRuntime.pushStack(value: BigtonValue) {
    error("TODO!")
}

fun BigtonRuntime.popStack(): BigtonValue? {
    error("TODO!")
}

fun BigtonRuntime.collectStack(): List<BigtonValue> {
    error("TODO!")
}

val BigtonRuntime.numConstStrings: Int
    get() = error("TODO!")

fun BigtonRuntime.getConstString(id: Int): String {
    error("TODO!")
}

var BigtonRuntime.error: BigtonRuntimeError
    get() = error("TODO!")
    set(value) {
        error("TODO!")
    }
    
val BigtonRuntime.currentFile: String
    get() = error("TODO!")
    
val BigtonRuntime.currentLine: Int
    get() = error("TODO!")
    
val BigtonRuntime.usedMemory: Long
    get() = error("TODO!")
    
val BigtonRuntime.awaitingBuiltinId: Int
    get() = error("TODO!")
    
fun BigtonRuntime.startTick() {
    error("TODO!")
}

fun BigtonRuntime.executeBatch(): BigtonExecStatus {
    // TODO! MAKE THE BUILTIN FUNCTION CONTAINED INSIDE BIGTON EXEC STATUS
    error("TODO!")
}