
#include "jni-headers/schwalbe_ventura_bigton_runtime_BigtonRuntime.h"
#include <bigton/runtime.h>
#include <bigton/values.h>
#include <bigton/error.h>
#include <bigton/ir.h>
#include <jni.h>
#include <stdlib.h>

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    createSettings
 * Signature: (JJII)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_createSettings(
    JNIEnv *env, jclass cls,
    jlong tickInstructionLimit,
    jlong memoryUsageLimit,
    jint maxCallDepth,
    jint maxTupleSize
) {
    (void) env, (void) cls;
    bigton_runtime_settings_t *settings
        = malloc(sizeof(bigton_runtime_settings_t));
    settings->tickInstructionLimit = (uint64_t) tickInstructionLimit;
    settings->memoryUsageLimit = (uint64_t) memoryUsageLimit;
    settings->maxCallDepth = (uint32_t) maxCallDepth;
    settings->maxTupleSize = (uint32_t) maxTupleSize;
    return (jlong) settings;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    freeSettings
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_freeSettings(
    JNIEnv *env, jclass cls,
    jlong rawSettings
) {
    (void) env, (void) cls;
    free((void *) rawSettings);
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    create
 * Signature: (JJJ)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_create(
    JNIEnv *env, jclass cls,
    jlong rawSettings, jlong rawProgram, jlong rawProgramLength
) {
    (void) env, (void) cls;
    bigton_runtime_settings_t *settings
        = (bigton_runtime_settings_t *) rawSettings;
    bigton_error_t parseError;
    bigton_parsed_program_t p;
    bigtonParseProgram(
        (const uint8_t *) rawProgram, (size_t) rawProgramLength,
        &p, &parseError
    );
    if (parseError != BIGTONE_NONE) { return (jlong) 0; }
    bigton_runtime_state_t *r
        = malloc(sizeof(bigton_runtime_state_t));
    bigtonInit(r, settings, &p);
    return (jlong) r;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    free
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_free(
    JNIEnv *env, jclass cls,
    jlong rawRuntime
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    bigtonFree(r);
    free((void *) r);
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    getLogLength
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_getLogLength(
    JNIEnv *env, jclass cls,
    jlong rawRuntime
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    return (jlong) r->logsCount;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    getLogString
 * Signature: (JJ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_getLogString(
    JNIEnv *env, jclass cls,
    jlong rawRuntime, jlong i
) {
    (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    bigton_string_t *line = r->logs[i];
    bigtonValRcIncr(BIGTON_STRING_VALUE(line));
    return (*env)->NewString(env, (const jchar *) line->content, line->length);
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    addLogLine
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_addLogLine(
    JNIEnv *env, jclass cls,
    jlong rawRuntime, jlong rawLineValue
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    bigton_tagged_value_t *lineValue = (bigton_tagged_value_t *) rawLineValue;
    if (lineValue->t != BIGTON_STRING) { return; }
    bigtonLogLine(r, lineValue->v.s);
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    getTraceLength
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_getTraceLength(
    JNIEnv *env, jclass cls, 
    jlong rawRuntime
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    return (jlong) r->traceCount;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    getTrace
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_getTrace(
    JNIEnv *env, jclass cls,
    jlong rawRuntime, jlong i
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    bigton_trace_call_t *t = malloc(sizeof(bigton_trace_call_t));
    *t = r->trace[i];
    return (jlong) t;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    getStackLength
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_getStackLength(
    JNIEnv *env, jclass cls,
    jlong rawRuntime
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    return (jlong) r->stack.count;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    pushStack
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_pushStack(
    JNIEnv *env, jclass cls,
    jlong rawRuntime, jlong rawValue
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    bigton_tagged_value_t *v = (bigton_tagged_value_t *) rawValue;
    bigtonStackPush(&r->stack, *v, r);
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    getStack
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_getStack(
    JNIEnv *env, jclass cls, 
    jlong rawRuntime, jlong i
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    bigton_tagged_value_t *v = malloc(sizeof(bigton_tagged_value_t));
    *v = bigtonStackAt(&r->stack, (size_t) i, r);
    bigtonValRcIncr(*v);
    return (jlong) v;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    popStack
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_popStack(
    JNIEnv *env, jclass cls,
    jlong rawRuntime
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    if (r->stack.count == 0) { return (jlong) 0; }
    bigton_tagged_value_t *v = malloc(sizeof(bigton_tagged_value_t));
    *v = bigtonStackPop(&r->stack, r);
    return (jlong) v;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    hasError
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_hasError(
    JNIEnv *env, jclass cls,
    jlong rawRuntime
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    return (jboolean) r->error != BIGTONE_NONE;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    getError
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_getError(
    JNIEnv *env, jclass cls,
    jlong rawRuntime
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    return (jint) r->error;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    getCurrentFile
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_getCurrentFile(
    JNIEnv *env, jclass cls,
    jlong rawRuntime
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    return (jint) r->currentSource.file;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    getCurrentLine
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_getCurrentLine(
    JNIEnv *env, jclass cls,
    jlong rawRuntime
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    return (jint) r->currentSource.line;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    getUsedMemory
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_getUsedMemory(
    JNIEnv *env, jclass cls,
    jlong rawRuntime
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    return (jlong) r->b.totalSizeBytes;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    getAwaitingBuiltinFun
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_getAwaitingBuiltinFun(
    JNIEnv *env, jclass cls,
    jlong rawRuntime
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    return (jint) r->awaitingBuiltinFun;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    getConstString
 * Signature: (JI)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_getConstString(
    JNIEnv *env, jclass cls,
    jlong rawRuntime, jint constStrId
) {
    (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    bigton_const_string_t *s = r->program.constStrings + constStrId;
    const bigton_char_t *content = r->program.constStringChars + s->firstOffset;
    return (*env)->NewString(env, (const jchar *) content, s->charLength);
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    startTick
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_startTick(
    JNIEnv *env, jclass cls,
    jlong rawRuntime
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    bigtonStartTick(r);
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonRuntime
 * Method:    execBatch
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonRuntime_execBatch(
    JNIEnv *env, jclass cls,
    jlong rawRuntime
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    bigton_exec_status_t status = bigtonExecBatch(r);
    return (jint) status;
}