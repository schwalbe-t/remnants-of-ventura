
#ifndef BIGTON_H
#define BIGTON_H

#include "bigton_ir.h"
#include "bigton_error.h"
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


static bigton_tagged_value_t bigtonTupleAt(
    bigton_tuple_t *t, size_t i, bigton_error_t *e
) {
    if (i >= t->length) {
        *e = BIGTONE_TUPLE_INDEX_OOB;
        return BIGTON_NULL_VALUE;
    }
    return (bigton_tagged_value_t) {
        .t = t->valueTypes[i],
        .v = t->values[i]
    };
}

static size_t bigtonObjectFindMem(
    bigton_object_t *o, bigton_str_id_t name,
    const bigton_shape_prop_t *allProps, size_t allPropCount,
    bigton_error_t *e
) {
    bigton_shape_t shape = *o->shape;
    bool inRange = shape.firstPropOffset <= allPropCount
        && shape.firstPropOffset + shape.propCount <= allPropCount; 
    if (!inRange) {
        *e = BIGTONE_INT_SHAPE_PROP_IDX_OOB;
        return 0;
    }
    const bigton_shape_prop_t *prop = allProps + shape.firstPropOffset;
    const bigton_shape_prop_t *propEnd = prop + shape.propCount;
    while (prop < propEnd) {
        if (prop->name == name) { return prop->offset; }
        prop += 1;
    }
    *e = BIGTONE_INVALID_OBJECT_MEMBER;
    return 0;
}

static bigton_tagged_value_t bigtonObjectAt(bigton_object_t *o, size_t i) {
    return (bigton_tagged_value_t) {
        .t = o->memberTypes[i],
        .v = o->memberValues[i]
    };
}

static bigton_tagged_value_t bigtonObjectSet(
    bigton_object_t *o, size_t i, bigton_tagged_value_t v
) {
    bigton_tagged_value_t oldMem = (bigton_tagged_value_t) {
        .t = o->memberTypes[i],
        .v = o->memberValues[i]
    };
    o->memberTypes[i] = v.t;
    o->memberValues[i] = v.v;
    return oldMem;
}

static bigton_tagged_value_t bigtonArrayAt(
    bigton_array_t *a, bigton_int_t i, bigton_error_t *e
) {
    if (i < 0 || (size_t) i >= a->length) {
        *e = BIGTONE_TUPLE_INDEX_OOB;
        return BIGTON_NULL_VALUE;
    }
    return (bigton_tagged_value_t) {
        .t = a->elementTypes[i],
        .v = a->elementValues[i]
    };
}

static bigton_tagged_value_t bigtonArraySet(
    bigton_array_t *a, bigton_int_t i, bigton_tagged_value_t v,
    bigton_error_t *e
) {
    if (i < 0 && (size_t) i >= a->length) {
        *e = BIGTONE_ARRAY_INDEX_OOB;
        return BIGTON_NULL_VALUE;
    }
    bigton_tagged_value_t oldValue = (bigton_tagged_value_t) {
        .t = a->elementTypes[i],
        .v = a->elementValues[i]
    };
    a->elementTypes[i] = v.t;
    a->elementValues[i] = v.v;
    return oldValue;
}


typedef struct BigtonBuff bigton_buff_t;
typedef struct BigtonBuffOwner bigton_buff_owner_t;

typedef struct BigtonBuff {
    bigton_buff_owner_t *owner;
    bigton_buff_t *prev;
    bigton_buff_t *next;
    size_t sizeBytes;
    const uint8_t data[];
} bigton_buff_t;

typedef struct BigtonBuffOwner {
    bigton_buff_t *first;
    bigton_buff_t *last;
    size_t totalSizeBytes;
} bigton_buff_owner_t;

void *bigtonAllocBuff(bigton_buff_owner_t *o, size_t numBytes);
static void *bigtonAllocNullableBuff(bigton_buff_owner_t *o, size_t numBytes) {
    if (numBytes == 0) { return NULL; }
    return bigtonAllocBuff(o, numBytes);
}

void bigtonFreeBuff(const void *buffer);
static void bigtonFreeNullableBuff(const void *buffer) {
    if (buffer == NULL) { return; }
    bigtonFreeBuff(buffer);
}

void *bigtonReallocBuff(
    const void *buffer, size_t numBytes
);
static void *bigtonReallocNullableBuff(
    bigton_buff_owner_t *o, const void *buffer, size_t numBytes
) {
    if (buffer == NULL) {
        return bigtonAllocNullableBuff(o, numBytes);
    }
    if (numBytes == 0) {
        bigtonFreeBuff(buffer);
        return NULL;
    }
    return bigtonReallocBuff(buffer, numBytes);
}

void bigtonFreeAll(bigton_buff_owner_t *o);


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