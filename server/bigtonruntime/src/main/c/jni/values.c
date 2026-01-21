
#include "generated/kotlin/main/schwalbe_ventura_bigton_runtime_BigtonValueN.h"
#include <bigton/values.h>
#include <bigton/runtime.h>
#include <stdlib.h>
#include <string.h>
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

// external fun getStringLength(handle: Long): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_getStringLength
PARAMS(jlong valueHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    return (jint) value->v.s->length;
}

// external fun concatStrings(
//     handleA: Long, handleB: Long, runtimeHandle: Long
// ): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_concatStrings
PARAMS(jlong handleA, jlong handleB, jlong runtimeHandle) {
    UNPACK(handleA, bigton_tagged_value_t, valueA);
    UNPACK(handleB, bigton_tagged_value_t, valueB);
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    bigton_string_t *a = valueA->v.s;
    bigton_string_t *b = valueB->v.s;
    uint32_t newLengthChars = a->length + b->length;
    size_t newLengthBytes = sizeof(bigton_char_t) * newLengthChars;
    bigton_char_t *newContent = bigtonAllocNullableBuff(&r->b, newLengthBytes);
    bigton_char_t *aDestStart = newContent;
    memcpy(aDestStart, a->content, sizeof(bigton_char_t) * a->length);
    bigton_char_t *bDestStart = aDestStart + a->length;
    memcpy(bDestStart, b->content, sizeof(bigton_char_t) * b->length);
    bigton_string_t *c = bigtonAllocBuff(&r->b, sizeof(bigton_string_t));
    c->rc = BIGTON_RC_INIT;
    c->content = newContent;
    c->length = newLengthChars;
    MALLOC_VALUE(valueC);
    valueC->t = BIGTON_STRING;
    valueC->v.s = c;
    return AS_HANDLE(valueC);
}

// external fun sliceString(
//     handle: Long, startIdx: Int, endIdx: Int, runtimeHandle: Long
// ): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_sliceString
PARAMS(jlong valueHandle, jint startIdx, jint endIdx, jlong runtimeHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    uint32_t resLength = endIdx - startIdx;
    const bigton_char_t *resContent = value->v.s->content + startIdx;
    MALLOC_VALUE(result);
    result->t = BIGTON_STRING;
    result->v.s = bigtonAllocString(&r->b, resLength, resContent);
    return AS_HANDLE(result);
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

// external fun getTupleFlatLength(handle: Long): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_getTupleFlatLength
PARAMS(jlong valueHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    return (jint) value->v.t->flatLength;
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

// external fun getObjectPropCount(handle: Long): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_getObjectPropCount
PARAMS(jlong valueHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    return (jint) value->v.o->shape->propCount;
}

// external fun findObjectProp(
//     handle: Long, name: String, runtimeHandle: Long
// ): Int
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_findObjectProp
PARAMS(jlong valueHandle, jstring jName, jlong runtimeHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    if (jName == NULL) { return -1; }
    jsize nameLength = (*env)->GetStringLength(env, jName);
    const jchar *nameChars = (*env)->GetStringChars(env, jName, NULL);
    if (nameChars == NULL) { return -1; }

    bigton_object_t *o = value->v.o;
    bigton_shape_t shape = *o->shape;
    jint foundPropId = -1;
    for (uint32_t propI = 0; propI < shape.propCount; propI += 1) {
        uint32_t absPropIdx = shape.firstPropOffset + propI;
        bigton_shape_prop_t prop = r->program.props[absPropIdx];
        bigton_const_string_t propName = r->program.constStrings[prop.name];
        const bigton_char_t *propNameChars
            = r->program.constStringChars + propName.firstOffset;
        if (propName.charLength != nameLength) { continue; }
        size_t lenBytes = nameLength * sizeof(bigton_char_t);
        if (memcmp(nameChars, propNameChars, lenBytes) == 0) {
            foundPropId = (jint) propI;
            break;
        }
    }

    (*env)->ReleaseStringChars(env, jName, nameChars);
    return foundPropId;
}

// external fun getObjectPropName(
//     handle: Long, propHandle: Int, runtimeHandle: Long
// ): String
JNIEXPORT jstring JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_getObjectPropName
PARAMS(jlong valueHandle, jint propId, jlong runtimeHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    bigton_shape_t shape = *value->v.o->shape;
    bigton_shape_prop_t prop = r->program.props[shape.firstPropOffset + propId];
    bigton_const_string_t propName = r->program.constStrings[prop.name];
    const bigton_char_t *propNameChars
        = r->program.constStringChars + propName.firstOffset;
    return (*env)->NewString(
        env, (const jchar *) propNameChars, (jsize) propName.charLength
    );
}

// external fun getObjectPropValue(
//     handle: Long, propHandle: Int
// ): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_getObjectPropValue
PARAMS(jlong valueHandle, jint propId) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    bigton_object_t *o = value->v.o;
    MALLOC_VALUE(containedValue);
    containedValue->t = o->memberTypes[propId];
    containedValue->v = o->memberValues[propId];
    bigtonValRcIncr(*containedValue);
    return AS_HANDLE(containedValue);
}

// external fun setObjectPropValue(
//     handle: Long, propHandle: Int, valueHandle: Long
// )
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_setObjectPropValue
PARAMS(jlong valueHandle, jint propId, jlong containedValueHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    UNPACK(containedValueHandle, bigton_tagged_value_t, containedValue);
    bigton_object_t *o = value->v.o;
    bigton_value_type_t *memTypes = (bigton_value_type_t *) o->memberTypes;
    bigton_value_t *memValues = (bigton_value_t *) o->memberValues;
    bigtonValRcDecr((bigton_tagged_value_t) {
        .t = memTypes[propId], .v = memValues[propId]
    });
    bigtonValRcIncr(*containedValue);
    memTypes[propId] = containedValue->t;
    memValues[propId] = containedValue->v;
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

// external fun insertArrayAt(
//     handle: Long, index: Int, valueHandle: Long, runtimeHandle: Long
// )
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_insertArrayAt
PARAMS(jlong valueHandle, jint i, jlong insertValueHandle, jlong runtimeHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    UNPACK(insertValueHandle, bigton_tagged_value_t, insertValue);
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    bigton_array_t *a = value->v.a;
    if (a->length + 1 > a->capacity) {
        a->capacity += 4;
        a->elementTypes = bigtonReallocNullableBuff(
            &r->b, a->elementTypes, sizeof(bigton_value_type_t) * a->capacity
        );
        a->elementValues = bigtonReallocNullableBuff(
            &r->b, a->elementValues, sizeof(bigton_value_t) * a->capacity
        );
    }
    bigton_value_type_t *elemTypes = a->elementTypes;
    bigton_value_t *elemValues = a->elementValues;
    uint32_t numShifted = a->length - i;
    if (numShifted != 0) {
        memmove(
            elemTypes + i + 1, elemTypes + i,
            sizeof(bigton_value_type_t) * numShifted
        );
        memmove(
            elemValues + i + 1, elemValues + i,
            sizeof(bigton_value_t) * numShifted
        );
    }
    bigtonValRcIncr(*insertValue);
    elemTypes[i] = insertValue->t;
    elemValues[i] = insertValue->v;
    a->length += 1;
}

// external fun removeArrayAt(handle: Long, index: Int): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_removeArrayAt
PARAMS(jlong valueHandle, jint i) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    bigton_array_t *a = value->v.a;
    bigton_value_type_t *elemTypes = a->elementTypes;
    bigton_value_t *elemValues = a->elementValues;
    MALLOC_VALUE(removed);
    removed->t = elemTypes[i];
    removed->v = elemValues[i];
    uint32_t numShifted = a->length - i - 1;
    if (numShifted != 0) {
        memmove(
            elemTypes + i, elemTypes + i + 1,
            sizeof(bigton_value_type_t) * numShifted
        );
        memmove(
            elemValues + i, elemValues + i + 1,
            sizeof(bigton_value_t) * numShifted
        );
    }
    a->length -= 1;
    return AS_HANDLE(removed);
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

// external fun concatArrays(
//     handleA: Long, handleB: Long, runtimeHandle: Long
// ): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_concatArrays
PARAMS(jlong handleA, jlong handleB, jlong runtimeHandle) {
    UNPACK(handleA, bigton_tagged_value_t, valueA);
    UNPACK(handleB, bigton_tagged_value_t, valueB);
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    bigton_array_t *a = valueA->v.a;
    bigton_array_t *b = valueB->v.a;
    uint32_t newLength = a->length + b->length;
    bigton_value_type_t *newElemTypes = bigtonAllocNullableBuff(
        &r->b, sizeof(bigton_value_type_t) * newLength
    );
    bigton_value_t *newElemValues = bigtonAllocNullableBuff(
        &r->b, sizeof(bigton_value_t) * newLength
    );
    for (size_t i = 0; i < a->length; i += 1) {
        bigton_value_type_t t = a->elementTypes[i];
        bigton_value_t v = a->elementValues[i];
        bigtonValRcIncr((bigton_tagged_value_t) { .t = t, .v = v });
        newElemTypes[i] = t;
        newElemValues[i] = v;
    }
    size_t bDestOffset = a->length;
    for (size_t i = 0; i < b->length; i += 1) {
        bigton_value_type_t t = b->elementTypes[i];
        bigton_value_t v = b->elementValues[i];
        bigtonValRcIncr((bigton_tagged_value_t) { .t = t, .v = v });
        size_t di = i + bDestOffset;
        newElemTypes[di] = t;
        newElemValues[di] = v;
    }
    bigton_array_t *c = bigtonAllocBuff(&r->b, sizeof(bigton_array_t));
    c->rc = BIGTON_RC_INIT;
    c->length = newLength;
    c->capacity = newLength;
    c->elementTypes = newElemTypes;
    c->elementValues = newElemValues;
    MALLOC_VALUE(valueC);
    valueC->t = BIGTON_ARRAY;
    valueC->v.a = c;
    return AS_HANDLE(valueC);
}

// external fun sliceArray(
//     handle: Long, startIdx: Int, endIdx: Int, runtimeHandle: Long
// ): Long
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValueN_sliceArray
PARAMS(jlong valueHandle, jint startIdx, jint endIdx, jlong runtimeHandle) {
    UNPACK(valueHandle, bigton_tagged_value_t, value);
    UNPACK(runtimeHandle, bigton_runtime_state_t, r);
    bigton_array_t *src = value->v.a;
    bigton_value_type_t *srcElemTypes = src->elementTypes;
    bigton_value_t *srcElemValues = src->elementValues;
    uint32_t resLength = endIdx - startIdx;
    bigton_value_type_t *resElemTypes = bigtonAllocNullableBuff(
        &r->b, sizeof(bigton_value_type_t) * resLength
    );
    bigton_value_t *resElemValues = bigtonAllocNullableBuff(
        &r->b, sizeof(bigton_value_t) * resLength
    );
    for(size_t di = 0; di < resLength; di += 1) {
        size_t i = startIdx + di;
        bigton_value_type_t t = srcElemTypes[i];
        bigton_value_t v = srcElemValues[i];
        bigtonValRcIncr((bigton_tagged_value_t) { .t = t, .v = v });
        resElemTypes[di] = t;
        resElemValues[di] = v;
    }
    bigton_array_t *res = bigtonAllocBuff(&r->b, sizeof(bigton_array_t));
    res->rc = BIGTON_RC_INIT;
    res->length = resLength;
    res->capacity = resLength;
    res->elementTypes = resElemTypes;
    res->elementValues = resElemValues;
    MALLOC_VALUE(resValue);
    resValue->t = BIGTON_ARRAY;
    resValue->v.a = res;
    return AS_HANDLE(resValue);
}