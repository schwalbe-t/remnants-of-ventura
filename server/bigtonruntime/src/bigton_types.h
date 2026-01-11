
#ifndef BIGTON_TYPES_H
#define BIGTON_TYPES_H

#include <stdint.h>

typedef uint32_t bigton_str_id_t;

typedef int64_t bigton_int_t;

typedef double bigton_float_t;

typedef struct BigtonShapeProp {
    bigton_str_id_t name;
    uint32_t byteOffset;
} bigton_shape_prop_t;
typedef struct BigtonShape {
    uint32_t propCount;
    bigton_shape_prop_t props[];
} bigton_shape_t;

#endif