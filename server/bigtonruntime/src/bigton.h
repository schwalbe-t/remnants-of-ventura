
#ifndef BIGTON_H
#define BIGTON_H

#include "bigton_ir.h"
#include <string.h>


typedef struct BigtonRc {
    int32_t count;
} bigton_rc_t;

#define BIGTON_RC_INIT ((bigton_rc_t) { .count = 1 })


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
    const bigton_char_t *content;
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

typedef struct BigtonTaggedValue {
    bigton_value_type_t t;
    bigton_value_t v;
} bigton_tagged_value_t;

#define BIGTON_NULL_VALUE ((bigton_tagged_value_t) { \
    .t = BIGTON_NULL, \
    .v = ((bigton_value_t) {}) \
})
#define BIGTON_INT_VALUE(value) ((bigton_tagged_value_t) { \
    .t = BIGTON_INT, \
    .v = ((bigton_value_t) { .i = (value) }) \
})
#define BIGTON_FLOAT_VALUE(value) ((bigton_tagged_value_t) { \
    .t = BIGTON_FLOAT, \
    .v = ((bigton_value_t) { .f = (value) }) \
})
#define BIGTON_STRING_VALUE(value) ((bigton_tagged_value_t) { \
    .t = BIGTON_STRING, \
    .v = ((bigton_value_t) { .s = (value) }) \
})
#define BIGTON_TUPLE_VALUE(value) ((bigton_tagged_value_t) { \
    .t = BIGTON_TUPLE, \
    .v = ((bigton_value_t) { .t = (value) }) \
})
#define BIGTON_OBJECT_VALUE(value) ((bigton_tagged_value_t) { \
    .t = BIGTON_OBJECT, \
    .v = ((bigton_value_t) { .o = (value) }) \
})
#define BIGTON_ARRAY_VALUE(value) ((bigton_tagged_value_t) { \
    .t = BIGTON_ARRAY, \
    .v = ((bigton_value_t) { .a = (value) }) \
})


void *bigtonAllocBuff(size_t numBytes);
static void *bigtonAllocNullableBuff(size_t numBytes) {
    if (numBytes == 0) { return NULL; }
    return bigtonAllocBuff(numBytes);
}

void bigtonFreeBuff(const void *buffer);
static void bigtonFreeNullableBuff(const void *buffer) {
    if (buffer == NULL) { return; }
    bigtonFreeBuff(buffer);
}

void *bigtonReallocBuff(const void *buffer, size_t numBytes);
static void *bigtonReallocNullableBuff(const void *buffer, size_t numBytes) {
    if (buffer == NULL) {
        return bigtonAllocNullableBuff(numBytes);
    }
    if (numBytes == 0) {
        bigtonFreeBuff(buffer);
        return NULL;
    }
    return bigtonReallocBuff(buffer, numBytes);
}

static void bigtonValRcIncr(bigton_tagged_value_t value) {
    switch (value.t) {
        case BIGTON_NULL:
        case BIGTON_INT:
        case BIGTON_FLOAT:
            break;
        case BIGTON_STRING:
            value.v.s->rc.count += 1;
            break;
        case BIGTON_TUPLE:
            value.v.t->rc.count += 1;
            break;
        case BIGTON_OBJECT:
            value.v.o->rc.count += 1;
            break;
        case BIGTON_ARRAY:
            value.v.a->rc.count += 1;
            break;
    }
}

void bigtonValFree(bigton_tagged_value_t value);

static void bigtonValRcDecr(bigton_tagged_value_t value) {
    int32_t newCount;
    switch (value.t) {
        case BIGTON_NULL:
        case BIGTON_INT:
        case BIGTON_FLOAT:
            return;
        case BIGTON_STRING:
            newCount = value.v.s->rc.count -= 1;
            break;
        case BIGTON_TUPLE:
            newCount = value.v.t->rc.count -= 1;
            break;
        case BIGTON_OBJECT:
            newCount = value.v.o->rc.count -= 1;
            break;
        case BIGTON_ARRAY:
            newCount = value.v.a->rc.count -= 1;
            break;
    }
    if (newCount <= 0) {
        bigtonValFree(value);
    }
}

#endif