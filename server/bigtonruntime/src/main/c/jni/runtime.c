
#include "generated/kotlin/main/schwalbe_ventura_bigton_runtime_BigtonRuntimeN.h"
#include <bigton/values.h>
#include <bigton/runtime.h>
#include <stdlib.h>
#include <string.h>
#include "helpers.h"

// external fun create(
//     rawProgramBuf: ByteBuffer,
//     rawProgramOffset: Int,
//     rawProgramLength: Int,
//     tickInstructionLimit: Long,
//     memoryUsageLimit: Long,
//     maxCallDepth: Int,
//     maxTupleSize: Int
// ): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_create
PARAMS(
    jobject rawProgramBuff, 
    jint rawProgramOffset,
    jint rawProgramLength,
    jlong tickInstructionLimit,
    jlong memoryUsageLimit,
    jint maxCallDepth,
    jint maxTupleSize
) {
    const uint8_t *rawJProgram
        = (*env)->GetDirectBufferAddress(env, rawProgramBuff)
        + (size_t) rawProgramOffset;
    const uint8_t *rawProgram = malloc((size_t) rawProgramLength);
    memcpy(rawProgram, rawJProgram, (size_t) rawProgramLength);
    bigton_runtime_settings_t settings = (bigton_runtime_settings_t) {
        .tickInstructionLimit = tickInstructionLimit,
        .memoryUsageLimit = memoryUsageLimit,
        .maxCallDepth = maxCallDepth,
        .maxTupleSize = maxTupleSize
    };
    bigton_parsed_program_t p;
    bigton_error_t parseError = BIGTONE_NONE;
    bigtonParseProgram(rawProgram, (size_t) rawProgramLength, &p, &parseError);
    bigton_runtime_state_t *r = malloc(sizeof(bigton_runtime_state_t));
    bigtonInit(r, &settings, &p);
    r->error = r->error || parseError;
    return AS_HANDLE(r);
}

// external fun free(runtimeHandle: Long)
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_free
PARAMS(jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    free(r->program.rawProgramBuffer);
    free(r);
}

// external fun debugLoadedProgram(runtimeHandle: Long)
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_debugLoadedProgram
PARAMS(jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    bigtonDebugProgram(&r->program);
}

// external fun addLogLine(
//     runtimeHandle: Long, stringValueHandle: Long
// )
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_addLogLine
PARAMS(jlong runtimeHandle, jlong stringValueHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    UNPACK(stringValueHandle, bigton_tagged_value_t, lineValue);
    bigtonValRcIncr(*lineValue);
    bigtonLogLine(r, lineValue->v.s);
}

// external fun getLogLineCount(runtimeHandle: Long): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getLogLineCount
PARAMS(jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    return (jint) r->logsCount;
}

// external fun getLogLineAt(runtimeHandle: Long, i: Int): String
JNIEXPORT jstring JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getLogLineAt
PARAMS(jlong runtimeHandle, jint i) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    bigton_string_t *s = r->logs[i];
    return (*env)->NewString(
        env, (const jchar *) s->content, (jsize) s->length
    );
}

// external fun clearLogLines(runtimeHandle: Long)
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_clearLogLines
PARAMS(jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    size_t count = r->logsCount;
    bigton_string_t **logs = r->logs;
    for (size_t i = 0; i < count; i += 1) {
        bigtonValRcDecr(BIGTON_STRING_VALUE(logs[i]));
    }
    r->logsCount = 0;
}

// external fun getBacktraceLength(runtimeHandle: Long): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getBacktraceLength
PARAMS(jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    return (jint) r->traceCount;
}

// external fun getBacktraceName(runtimeHandle: Long, i: Int): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getBacktraceName
PARAMS(jlong runtimeHandle, jint i) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    return (jint) r->trace[i].name;
}

// external fun getBacktraceDeclFile(runtimeHandle: Long, i: Int): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getBacktraceDeclFile
PARAMS(jlong runtimeHandle, jint i) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    return (jint) r->trace[i].definedAt.file;
}

// external fun getBacktraceDeclLine(runtimeHandle: Long, i: Int): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getBacktraceDeclLine
PARAMS(jlong runtimeHandle, jint i) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    return (jint) r->trace[i].definedAt.line;
}

// external fun getBacktraceFromFile(runtimeHandle: Long, i: Int): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getBacktraceFromFile
PARAMS(jlong runtimeHandle, jint i) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    return (jint) r->trace[i].calledFrom.file;
}

// external fun getBacktraceFromLine(runtimeHandle: Long, i: Int): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getBacktraceFromLine
PARAMS(jlong runtimeHandle, jint i) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    return (jint) r->trace[i].calledFrom.line;
}

// external fun stackPush(runtimeHandle: Long, valueHandle: Long)
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_stackPush
PARAMS(jlong runtimeHandle, jlong valueHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    bigtonValRcIncr(*value);
    bigtonStackPush(&r->stack, *value, r);
}

// external fun stackPop(runtimeHandle: Long): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_stackPop
PARAMS(jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    if (r->stack.count == 0) { return 0; }
    MALLOC_VALUE(value);
    *value = bigtonStackPop(&r->stack, r);
    return AS_HANDLE(value);
}

// external fun getStackLength(runtimeHandle: Long): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getStackLength
PARAMS(jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    return (jint) r->stack.count;
}

// external fun getStackAt(runtimeHandle: Long, i: Int): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getStackAt
PARAMS(jlong runtimeHandle, jint i) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    MALLOC_VALUE(value);
    *value = bigtonStackAt(&r->stack, (size_t) i, r);
    bigtonValRcIncr(*value);
    return AS_HANDLE(value);
}

// external fun getNumConstStrings(runtimeHandle: Long): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getNumConstStrings
PARAMS(jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    return (jint) r->program.numConstStrings;
}

// external fun getConstString(runtimeHandle: Long, i: Int): String
JNIEXPORT jstring JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getConstString
PARAMS(jlong runtimeHandle, jint i) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    bigton_const_string_t s = r->program.constStrings[i];
    const bigton_char_t *content = r->program.constStringChars + s.firstOffset;
    return (*env)->NewString(
        env, (const jchar *) content, (jsize) s.charLength
    );
}

// external fun getCurrentFile(runtimeHandle: Long): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getCurrentFile
PARAMS(jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    return (jint) r->currentSource.file;
}

// external fun getCurrentLine(runtimeHandle: Long): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getCurrentLine
PARAMS(jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    return (jint) r->currentSource.line;
}

// external fun getUsedMemory(runtimeHandle: Long): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getUsedMemory
PARAMS(jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    return (jlong) r->b.totalSizeBytes;
}

// external fun getUsedInstrCost(runtimeHandle: Long): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getUsedInstrCost
PARAMS(jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    return (jlong) r->accCost;
}

// external fun getError(runtimeHandle: Long): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getError
PARAMS(jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    return (jint) r->error;
}

// external fun setError(runtimeHandle: Long, error: Int)
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_setError
PARAMS(jlong runtimeHandle, jint error) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    r->error = (bigton_error_t) error;
}

// external fun getAwaitingBuiltinId(runtimeHandle: Long): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_getAwaitingBuiltinId
PARAMS(jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    return (jint) r->awaitingBuiltinFun;
}

// external fun startTick(runtimeHandle: Long)
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_startTick
PARAMS(jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    bigtonStartTick(r);
}

// external fun executeBatch(runtimeHandle: Long): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntimeN_executeBatch
PARAMS(jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    return (jint) bigtonExecBatch(r);
}