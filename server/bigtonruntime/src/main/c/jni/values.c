
#include "generated/kotlin/main/schwalbe_ventura_bigton_runtime_BigtonValueN.h"
#include <bigton/values.h>
#include <bigton/runtime.h>
#include <stdlib.h>
#include "helpers.h"

// external fun free(handle: Long)
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_free
PARAMS(jlong valueHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    bigtonValRcDecr(*value);
    free(value);
}

// external fun getType(handle: Long): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_getType
PARAMS(jlong valueHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    return (jint) value->t;
}

// external fun createNull(): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_createNull
NO_PARAMS() {
    MALLOC_VALUE(value);
    value->t = BIGTON_NULL;
    return AS_HANDLE(value);
}

// external fun createInt(value: Long): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_createInt
PARAMS(jlong containedValue) {
    MALLOC_VALUE(value);
    value->t = BIGTON_INT;
    value->v.i = (bigton_int_t) containedValue;
    return AS_HANDLE(value);
}

// external fun getInt(handle: Long): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_getInt
PARAMS(jlong valueHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    return (jlong) value->v.i;
}

// external fun createFloat(value: Double): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_createFloat
PARAMS(jdouble containedValue) {
    MALLOC_VALUE(value);
    value->t = BIGTON_FLOAT;
    value->v.f = (bigton_float_t) containedValue;
    return AS_HANDLE(value);
}

// external fun getFloat(handle: Long): Double
JNIEXPORT jdouble JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_getFloat
PARAMS(jlong valueHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    return (jdouble) value->v.f;
}

// external fun createString(value: String, runtimeHandle: Long): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_createString
PARAMS(jstring containedValue, jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    if (containedValue == NULL) { return 0; }
    jsize charLength = (*env)->GetStringLength(env, containedValue);
    const jchar *chars = (*env)->GetStringChars(env, containedValue, NULL);
    if (chars == NULL) { return 0; }
    bigton_string_t *s = bigtonAllocString(
        &r->b, (uint64_t) charLength, (const bigton_char_t *) chars
    );
    (*env)->ReleaseStringChars(env, containedValue, chars);
    MALLOC_VALUE(value);
    value->t = BIGTON_STRING;
    value->v.s = s;
    return AS_HANDLE(value);
}

// external fun getString(handle: Long): String
JNIEXPORT jstring JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_getString
PARAMS(jlong valueHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    bigton_string_t *s = value->v.s;
    return (*env)->NewString(env, (const jchar *) s->content, (jsize) s->length);
}

// external fun createTuple(length: Int, runtimeHandle: Long): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_createTuple
PARAMS(jint length, jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    bigton_value_type_t *valueTypes = bigtonAllocNullableBuff(
        &r->b, sizeof(bigton_value_type_t) * (size_t) length
    );
    bigton_value_t *values = bigtonAllocNullableBuff(
        &r->b, sizeof(bigton_value_t) * (size_t) length
    );
    for (size_t i = 0; i < (size_t) length; i += 1) {
        valueTypes[i] = BIGTON_NULL;
    }
    bigton_tuple_t *t = bigtonAllocBuff(&r->b, sizeof(bigton_tuple_t));
    t->rc = BIGTON_RC_INIT;
    t->flatLength = (uint32_t) length;
    t->length = (uint32_t) length;
    t->valueTypes = valueTypes;
    t->values = values;
    MALLOC_VALUE(value);
    value->t = BIGTON_TUPLE;
    value->v.t = t;
    return AS_HANDLE(value);
}

// external fun setTupleAt(handle: Long, index: Int, valueHandle: Long)
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_setTupleAt
PARAMS(jlong valueHandle, jint i, jlong assignedValueHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    UNPACK(assignedValueHandle, bigton_tagged_value_t, assignedValue);
    bigton_tuple_t *t = value->v.t;
    bigton_value_type_t *valueTypes = (bigton_value_type_t *) t->valueTypes;
    bigton_value_t *values = (bigton_value_t *) t->values;
    bigtonValRcDecr((bigton_tagged_value_t) {
        .t = valueTypes[i], .v = values[i]
    });
    bigtonValRcIncr(*assignedValue);
    valueTypes[i] = assignedValue->t;
    values[i] = assignedValue->v;
}

// external fun completeTuple(handle: Long)
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_completeTuple
PARAMS(jlong valueHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    bigton_tuple_t *t = value->v.t;
    const bigton_value_type_t *valueTypes = t->valueTypes;
    const bigton_value_t *values = t->values;
    uint32_t length = t->length;
    uint32_t flatLength = 0;
    for (size_t i = 0; i < length; i += 1) {
        flatLength += valueTypes[i] == BIGTON_TUPLE
            ? values[i].t->flatLength
            : 1;
    }
    t->flatLength = flatLength;
}

// external fun getTupleLength(handle: Long): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_getTupleLength
PARAMS(jlong valueHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    return (jint) value->v.t->length;
}

// external fun getTupleAt(handle: Long, index: Int): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_getTupleAt
PARAMS(jlong valueHandle, jint i) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    bigton_tuple_t *t = value->v.t;
    MALLOC_VALUE(containedValue);
    containedValue->t = t->valueTypes[i];
    containedValue->v = t->values[i];
    bigtonValRcIncr(*containedValue);
    return AS_HANDLE(containedValue);
}

// external fun createArray(length: Int, runtimeHandle: Long): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_createArray
PARAMS(jint length, jlong runtimeHandle) {
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    bigton_value_type_t *valueTypes = bigtonAllocNullableBuff(
        &r->b, sizeof(bigton_value_type_t) * (size_t) length
    );
    bigton_value_t *values = bigtonAllocNullableBuff(
        &r->b, sizeof(bigton_value_t) * (size_t) length
    );
    for (size_t i = 0; i < (size_t) length; i += 1) {
        valueTypes[i] = BIGTON_NULL;
    }
    bigton_array_t *a = bigtonAllocBuff(&r->b, sizeof(bigton_array_t));
    a->rc = BIGTON_RC_INIT;
    a->capacity = (uint32_t) length;
    a->length = (uint32_t) length;
    a->elementTypes = valueTypes;
    a->elementValues = values;
    MALLOC_VALUE(value);
    value->t = BIGTON_ARRAY;
    value->v.a = a;
    return AS_HANDLE(value);
}

// external fun getArrayLength(handle: Long): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_getArrayLength
PARAMS(jlong valueHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    return (jint) value->v.a->length;
}

// external fun setArrayAt(handle: Long, index: Int, valueHandle: Long)
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_setArrayAt
PARAMS(jlong valueHandle, jint i, jlong containedValueHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    UNPACK(containedValueHandle, bigton_tagged_value_t, containedValue);
    bigton_array_t *a = value->v.a;
    bigton_value_type_t *elemTypes = (bigton_value_type_t *) a->elementTypes;
    bigton_value_t *elemValues = (bigton_value_t *) a->elementValues;
    bigtonValRcDecr((bigton_tagged_value_t) {
        .t = elemTypes[i], .v = elemValues[i]
    });
    bigtonValRcIncr(*containedValue);
    elemTypes[i] = containedValue->t;
    elemValues[i] = containedValue->v;
}

// external fun getArrayAt(handle: Long, index: Int): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_getArrayAt
PARAMS(jlong valueHandle, jint i) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    bigton_array_t *a = value->v.a;
    MALLOC_VALUE(containedValue);
    containedValue->t = a->elementTypes[i];
    containedValue->v = a->elementValues[i];
    bigtonValRcIncr(*containedValue);
    return AS_HANDLE(containedValue);
}