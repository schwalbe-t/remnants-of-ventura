
#include "jni/schwalbe_ventura_bigton_runtime_BigtonValue.h"
#include <bigton/runtime.h>
#include <bigton/values.h>
#include <stdlib.h>

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    copy
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_copy(
    JNIEnv *env, jclass cls,
    jlong rawValue
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *old = (bigton_tagged_value_t *) rawValue;
    bigton_tagged_value_t value = *old;
    bigton_tagged_value_t *new = malloc(sizeof(bigton_tagged_value_t));
    *new = value;
    bigtonValRcIncr(value);
    return (jlong) new;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    free
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_free(
    JNIEnv *env, jclass cls,
    jlong rawValue
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *v = (bigton_tagged_value_t *) rawValue;
    bigtonValRcDecr(*v);
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    getType
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_getType(
    JNIEnv *env, jclass cls,
    jlong rawValue
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *v = (bigton_tagged_value_t *) rawValue;
    return (jint) v->t;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    createNull
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_createNull(
    JNIEnv *env, jclass cls
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *v = malloc(sizeof(bigton_tagged_value_t));
    *v = BIGTON_NULL_VALUE;
    return (jlong) v;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    createInt
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_createInt(
    JNIEnv *env, jclass cls,
    jlong i
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *v = malloc(sizeof(bigton_tagged_value_t));
    *v = BIGTON_INT_VALUE((bigton_int_t) i);
    return (jlong) v;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    getInt
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_getInt(
    JNIEnv *env, jclass cls,
    jlong rawValue
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *v = (bigton_tagged_value_t *) rawValue;
    return (jlong) v->v.i;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    createFloat
 * Signature: (D)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_createFloat(
    JNIEnv *env, jclass cls,
    jdouble f
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *v = malloc(sizeof(bigton_tagged_value_t));
    *v = BIGTON_FLOAT_VALUE((bigton_float_t) f);
    return (jlong) v;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    getFloat
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_getFloat(
    JNIEnv *env, jclass cls,
    jlong rawValue
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *v = (bigton_tagged_value_t *) rawValue;
    return (jdouble) v->v.f;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    createString
 * Signature: (Ljava/lang/String;J)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_createString(
    JNIEnv *env, jclass cls,
    jstring js, jlong rawRuntime
) {
    (void) cls;
    if (js == NULL) { return 0; }
    jsize jCharLength = (*env)->GetStringLength(env, js);
    const jchar *jContent = (*env)->GetStringChars(env, js, NULL);
    if (jContent == NULL) { return 0; }
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    bigton_string_t *s = bigtonAllocString(
        &r->b, (size_t) jCharLength, (const bigton_char_t *) jContent
    );
    (*env)->ReleaseStringChars(env, js, jContent);
    bigton_tagged_value_t *v = malloc(sizeof(bigton_tagged_value_t));
    *v = BIGTON_STRING_VALUE(s);
    return (jlong) v;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    getString
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_getString(
    JNIEnv *env, jclass cls,
    jlong rawValue
) {
    (void) cls;
    bigton_tagged_value_t *v = (bigton_tagged_value_t *) rawValue;
    bigton_string_t *s = v->v.s;
    return (*env)->NewString(env, (const jchar *) s->content, s->length);
}

// TODO! createTuple
// TODO! getTupleLength
// TODO! getTupleMember
// TODO! setTupleMember

// TODO! findObjectPropDyn
// TODO! findObjectPropConst
// TODO! getObjectSize
// TODO! getObjectMember
// TODO! setObjectMember

// TODO! createArray
// TODO! getArrayLength
// TODO! getArrayElement
// TODO! setArrayElement