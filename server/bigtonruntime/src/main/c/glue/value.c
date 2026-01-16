
#include "jni-headers/schwalbe_ventura_bigton_runtime_BigtonValue.h"
#include <bigton/runtime.h>
#include <bigton/values.h>
#include <bigton/ir.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>

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
    free((void *) v);
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

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    createTuple
 * Signature: (IJ)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_createTuple(
    JNIEnv *env, jclass cls,
    jint length, jlong rawRuntime
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    bigton_tuple_t *t = bigtonAllocBuff(&r->b, sizeof(bigton_tuple_t));
    t->rc = BIGTON_RC_INIT;
    t->length = (uint32_t) length;
    t->flatLength = (uint32_t) length;
    bigton_value_type_t *elemTypes = bigtonAllocNullableBuff(
        &r->b, sizeof(bigton_value_type_t) * (size_t) length
    );
    for (size_t i = 0; i < (size_t) length; i += 1) {
        elemTypes[i] = BIGTON_NULL;
    }
    t->valueTypes = elemTypes;
    t->values = bigtonAllocNullableBuff(
        &r->b, sizeof(bigton_value_t) * (size_t) length
    );
    bigton_tagged_value_t *v = malloc(sizeof(bigton_tagged_value_t));
    *v = BIGTON_TUPLE_VALUE(t);
    return (jlong) v;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    getTupleLength
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_getTupleLength(
    JNIEnv *env, jclass cls,
    jlong rawValue
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *t = (bigton_tagged_value_t *) rawValue;
    return (jint) t->v.t->length;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    getTupleMember
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_getTupleMember(
    JNIEnv *env, jclass cls,
    jlong rawValue, jint i
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *tv = (bigton_tagged_value_t *) rawValue;
    bigton_tuple_t *t = tv->v.t;
    bigton_tagged_value_t *ev = malloc(sizeof(bigton_tagged_value_t));
    *ev = (bigton_tagged_value_t) {
        .t = t->valueTypes[i],
        .v = t->values[i]
    };
    bigtonValRcIncr(*ev);
    return (jlong) ev;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    setTupleMember
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_setTupleMember(
    JNIEnv *env, jclass cls,
    jlong rawValue, jint i, jlong rawElemValue
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *tv = (bigton_tagged_value_t *) rawValue;
    bigton_tagged_value_t *ev = (bigton_tagged_value_t *) rawElemValue;
    bigton_tuple_t *t = tv->v.t;
    bigton_value_type_t *elemTypes = (bigton_value_type_t *) t->valueTypes;
    bigton_value_t *elemValues = (bigton_value_t *) t->values;
    bigton_tagged_value_t oldElemValue = (bigton_tagged_value_t) {
        .t = elemTypes[i], .v = elemValues[i]
    };
    bigtonValRcIncr(*ev);
    elemTypes[i] = ev->t;
    elemValues[i] = ev->v;
    bigtonValRcDecr(oldElemValue);
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    updateTupleInfo
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_updateTupleInfo(
    JNIEnv *env, jclass cls,
    jlong rawValue
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *tv = (bigton_tagged_value_t *) rawValue;
    bigton_tuple_t *t = tv->v.t;
    size_t flatLength = 0;
    for (size_t i = 0; i < t->length; i += 1) {
        flatLength += t->valueTypes[i] == BIGTON_TUPLE
            ? t->values[i].t->flatLength
            : 1;
    }
    t->flatLength = flatLength;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    findObjectPropDyn
 * Signature: (JLjava/lang/String;J)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_findObjectPropDyn(
    JNIEnv *env, jclass cls,
    jlong rawValue, jstring jname, jlong rawRuntime
) {
    (void) cls;
    bigton_tagged_value_t *ov = (bigton_tagged_value_t *) rawValue;
    if (jname == NULL) { return 0; }
    uint64_t jnameLength = (uint64_t) (*env)->GetStringLength(env, jname);
    size_t jnameNumBytes = sizeof(uint16_t) * jnameLength;
    const jchar *jnameChars = (*env)->GetStringChars(env, jname, NULL);
    if (jnameChars == NULL) { return 0; }
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    uint32_t allPropCount = r->program.numProps;
    size_t allConstCharCount = r->program.numConstStringChars;
    const bigton_shape_t *shape = ov->v.o->shape;
    uint32_t shapePropCount = shape->propCount;
    bool propsInRange = shape->firstPropOffset <= allPropCount
        && shape->firstPropOffset + shape->propCount <= allPropCount;
    if (!propsInRange) { return 0; }
    const bigton_shape_prop_t *props
        = r->program.props + shape->firstPropOffset;
    jint foundPropId = -1;
    for (uint32_t propId = 0; propId < shapePropCount; propId += 1) {
        const bigton_shape_prop_t *prop = props + propId;
        bigton_str_id_t propNameId = prop->name;
        if (propNameId >= r->program.numConstStrings) { continue; }
        const bigton_const_string_t propName
            = r->program.constStrings[propNameId];
        if (propName.charLength != (uint64_t) jnameLength) { continue; }
        bool nameInRange = propName.firstOffset <= allConstCharCount
            && propName.firstOffset + propName.charLength <= allConstCharCount;
        if (!nameInRange) { continue; }
        const bigton_char_t *propNameChars
            = r->program.constStringChars + propName.firstOffset;
        if (memcmp(propNameChars, jnameChars, jnameNumBytes) == 0) {
            foundPropId = (jint) propId;
            break;
        }
    }
    (*env)->ReleaseStringChars(env, jname, jnameChars);
    return foundPropId;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    findObjectPropConst
 * Signature: (JIJ)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_findObjectPropConst(
    JNIEnv *env, jclass cls,
    jlong rawValue, jint nameId, jlong rawRuntime
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *ov = (bigton_tagged_value_t *) rawValue;
    bigton_str_id_t constNameId = (bigton_str_id_t) nameId;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    return bigtonObjectFindMem(
        ov->v.o, constNameId, r->program.props, r->program.numProps, &r->error
    );
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    getObjectSize
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_getObjectSize(
    JNIEnv *env, jclass cls,
    jlong rawValue
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *ov = (bigton_tagged_value_t *) rawValue;
    return (jint) ov->v.o->shape->propCount;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    getObjectPropName
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_getObjectPropName(
    JNIEnv *env, jclass cls,
    jlong rawValue, jint propId, jlong rawRuntime
) {
    bigton_tagged_value_t *ov = (bigton_tagged_value_t *) rawValue;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    bigton_object_t *o = ov->v.o;
    const bigton_shape_t *s = o->shape;
    size_t glPropId = s->firstPropOffset + (size_t) propId;
    if (glPropId >= r->program.numProps) { return (jint) -1; }
    return r->program.props[glPropId].name;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    getObjectMember
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_getObjectMember(
    JNIEnv *env, jclass cls,
    jlong rawValue, jint propId
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *ov = (bigton_tagged_value_t *) rawValue;
    bigton_object_t *o = ov->v.o;
    bigton_value_type_t *propTypes = o->memberTypes;
    bigton_value_t *propValues = o->memberValues;
    bigton_tagged_value_t *mv = malloc(sizeof(bigton_tagged_value_t));
    *mv = (bigton_tagged_value_t) {
        .t = propTypes[propId], .v = propValues[propId]
    };
    bigtonValRcIncr(*mv);
    return (jlong) mv;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    setObjectMember
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_setObjectMember(
    JNIEnv *env, jclass cls,
    jlong rawValue, jint propId, jlong rawMemValue
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *ov = (bigton_tagged_value_t *) rawValue;
    bigton_tagged_value_t *mv = (bigton_tagged_value_t *) rawMemValue;
    bigton_object_t *o = ov->v.o;
    bigton_value_type_t *propTypes = o->memberTypes;
    bigton_value_t *propValues = o->memberValues;
    bigton_tagged_value_t oldMemValue = (bigton_tagged_value_t) {
        .t = propTypes[propId], .v = propValues[propId]
    };
    bigtonValRcIncr(*mv);
    propTypes[propId] = mv->t;
    propValues[propId] = mv->v;
    bigtonValRcDecr(oldMemValue);
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    createArray
 * Signature: (IJ)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_createArray(
    JNIEnv *env, jclass cls,
    jint length, jlong rawRuntime
) {
    (void) env, (void) cls;
    bigton_runtime_state_t *r = (bigton_runtime_state_t *) rawRuntime;
    bigton_array_t *a = bigtonAllocBuff(&r->b, sizeof(bigton_array_t));
    a->rc = BIGTON_RC_INIT;
    a->length = (uint32_t) length;
    a->capacity = (uint32_t) length;
    bigton_value_type_t *elemTypes = bigtonAllocNullableBuff(
        &r->b, sizeof(bigton_value_type_t) * (size_t) length
    );
    for (size_t i = 0; i < (size_t) length; i += 1) {
        elemTypes[i] = BIGTON_NULL;
    }
    a->elementTypes = elemTypes;
    a->elementValues = bigtonAllocNullableBuff(
        &r->b, sizeof(bigton_value_t) * (size_t) length
    );
    bigton_tagged_value_t *v = malloc(sizeof(bigton_tagged_value_t));
    *v = BIGTON_ARRAY_VALUE(a);
    return (jlong) v;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    getArrayLength
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_getArrayLength(
    JNIEnv *env, jclass cls,
    jlong rawValue
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *av = (bigton_tagged_value_t *) rawValue;
    bigton_array_t *a = av->v.a;
    return (jint) a->length;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    getArrayElement
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_getArrayElement(
    JNIEnv *env, jclass cls,
    jlong rawValue, jint i
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *av = (bigton_tagged_value_t *) rawValue;
    bigton_array_t *a = av->v.a;
    bigton_tagged_value_t *ev = malloc(sizeof(bigton_tagged_value_t));
    *ev = (bigton_tagged_value_t) {
        .t = a->elementTypes[i],
        .v = a->elementValues[i]
    };
    bigtonValRcIncr(*ev);
    return (jlong) ev;
}

/*
 * Class:     schwalbe_ventura_bigton_runtime_BigtonValue
 * Method:    setArrayElement
 * Signature: (JIJ)V
 */
JNIEXPORT void JNICALL Java_schwalbe_ventura_bigton_runtime_BigtonValue_setArrayElement(
    JNIEnv *env, jclass cls,
    jlong rawValue, jint i, jlong rawElemValue
) {
    (void) env, (void) cls;
    bigton_tagged_value_t *av = (bigton_tagged_value_t *) rawValue;
    bigton_tagged_value_t *ev = (bigton_tagged_value_t *) rawElemValue;
    bigton_array_t *a = av->v.a;
    bigton_value_type_t *elemTypes = a->elementTypes;
    bigton_value_t *elemValues = a->elementValues;
    bigton_tagged_value_t oldElemValue = (bigton_tagged_value_t) {
        .t = elemTypes[i], .v = elemValues[i]
    };
    bigtonValRcIncr(*ev);
    elemTypes[i] = ev->t;
    elemValues[i] = ev->v;
    bigtonValRcDecr(oldElemValue);
}