
#include "jni/schwalbe_ventura_bigton_runtime_BigtonRuntime.h"
#include <bigton/runtime.h>
#include <bigton/values.h>
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