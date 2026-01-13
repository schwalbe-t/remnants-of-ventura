
#include "jni/schwalbe_ventura_bigton_runtime_BigtonTrace.h"
#include <bigton/runtime.h>
#include <stdlib.h>

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonTrace
 * Method:    getName
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonTrace_getName(
    JNIEnv *env, jclass cls,
    jlong rawTrace
) {
    (void) env, (void) cls;
    bigton_trace_call_t *t = (bigton_trace_call_t *) rawTrace;
    return (jint) t->name;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonTrace
 * Method:    getDeclFile
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonTrace_getDeclFile(
    JNIEnv *env, jclass cls,
    jlong rawTrace
) {
    (void) env, (void) cls;
    bigton_trace_call_t *t = (bigton_trace_call_t *) rawTrace;
    return (jint) t->definedAt.file;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonTrace
 * Method:    getDeclLine
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonTrace_getDeclLine(
    JNIEnv *env, jclass cls,
    jlong rawTrace
) {
    (void) env, (void) cls;
    bigton_trace_call_t *t = (bigton_trace_call_t *) rawTrace;
    return (jint) t->definedAt.line;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonTrace
 * Method:    getFromFile
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonTrace_getFromFile(
    JNIEnv *env, jclass cls,
    jlong rawTrace
) {
    (void) env, (void) cls;
    bigton_trace_call_t *t = (bigton_trace_call_t *) rawTrace;
    return (jint) t->calledFrom.file;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonTrace
 * Method:    getFromLine
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonTrace_getFromLine(
    JNIEnv *env, jclass cls,
    jlong rawTrace
) {
    (void) env, (void) cls;
    bigton_trace_call_t *t = (bigton_trace_call_t *) rawTrace;
    return (jint) t->calledFrom.line;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonTrace
 * Method:    free
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonTrace_free(
    JNIEnv *env, jclass cls,
    jlong rawTrace
) {
    (void) env, (void) cls;
    free((void *) rawTrace);
}