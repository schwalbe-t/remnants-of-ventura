
#ifndef BIGTON_H
#define BIGTON_H

#include <stddef.h>

#include "bigton_ir.h"


typedef struct BigtonRc {
    int32_t count;
} bigton_rc_t;


typedef enum BigtonValueType {
    BIGTON_NULL,
    BIGTON_INT,
    BIGTON_FLOAT,
    BIGTON_STRING,
    BIGTON_TUPLE,
    BIGTON_OBJECT,
    BIGTON_ARRAY
} bigton_value_type_t;


typedef union BigtonValue bigton_value_t;

typedef struct BigtonString {
    bigton_rc_t rc;
    uint32_t length;
    const uint8_t *value;
} bigton_string_t;

typedef struct BigtonTuple {
    bigton_rc_t rc;
    uint32_t length;
    uint32_t flatLength;
    const bigton_value_type_t *valueTypes;
    const bigton_value_t *values;
} bigton_tuple_t;

typedef struct BigtonObject {
    bigton_rc_t rc;
    const bigton_shape_t *shape;
    bigton_value_type_t *memberTypes;
    bigton_value_t *memberValues;
} bigton_object_t;

typedef struct BigtonArray {
    bigton_rc_t rc;
    uint32_t capacity;
    uint32_t length;
    bigton_value_type_t *elementTypes;
    bigton_value_t *elementValues;
} bigton_array_t;

typedef union BigtonValue {
    bigton_int_t i;
    bigton_float_t f;
    bigton_string_t *s;
    bigton_tuple_t *t;
    bigton_object_t *o;
    bigton_array_t *a;
} bigton_value_t;


void *bigtonAllocBuff(size_t numBytes);
inline void *bigtonAllocNullableBuff(size_t numBytes) {
    if (numBytes == 0) { return NULL; }
    return bigtonAllocBuff(numBytes);
}

void bigtonFreeBuff(const void *buffer);
inline void bigtonFreeNullableBuff(const void *buffer) {
    if (buffer == NULL) { return; }
    bigtonFreeBuff(buffer);
}

inline void bigtonValRcIncr(bigton_value_type_t type, bigton_value_t value) {
    switch (type) {
        case BIGTON_NULL:
        case BIGTON_INT:
        case BIGTON_FLOAT:
            break;
        case BIGTON_STRING:
            value.s->rc.count += 1;
            break;
        case BIGTON_TUPLE:
            value.t->rc.count += 1;
            break;
        case BIGTON_OBJECT:
            value.o->rc.count += 1;
            break;
        case BIGTON_ARRAY:
            value.a->rc.count += 1;
            break;
    }
}

inline void bigtonValFree(bigton_value_type_t type, bigton_value_t value) {
    switch (type) {
        case BIGTON_NULL:
        case BIGTON_INT:
        case BIGTON_FLOAT:
            return;
        case BIGTON_STRING:
            bigtonFreeNullableBuff(value.s->value);
            bigtonFreeBuff(value.s);
            break;
        case BIGTON_TUPLE:
            bigtonFreeBuff(value.t->valueTypes);
            bigtonFreeBuff(value.t->values);
            bigtonFreeBuff(value.t);
            break;
        case BIGTON_OBJECT:
            bigtonFreeNullableBuff(value.o->memberTypes);
            bigtonFreeNullableBuff(value.o->memberValues);
            bigtonFreeBuff(value.o);
            break;
        case BIGTON_ARRAY:
            bigtonFreeNullableBuff(value.a->elementTypes);
            bigtonFreeNullableBuff(value.a->elementValues);
            bigtonFreeBuff(value.a);
            break;
    }
}

inline void bigtonValRcDecr(bigton_value_type_t type, bigton_value_t value) {
    int32_t newCount;
    switch (type) {
        case BIGTON_NULL:
        case BIGTON_INT:
        case BIGTON_FLOAT:
            return;
        case BIGTON_STRING:
            newCount = value.s->rc.count -= 1;
            break;
        case BIGTON_TUPLE:
            newCount = value.t->rc.count -= 1;
            break;
        case BIGTON_OBJECT:
            newCount = value.o->rc.count -= 1;
            break;
        case BIGTON_ARRAY:
            newCount = value.a->rc.count -= 1;
            break;
    }
    if (newCount <= 0) {
        bigtonValFree(type, value);
    }
}

#endif