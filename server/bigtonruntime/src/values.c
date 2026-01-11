
#include "bigton.h"
#include "bigton_ir.h"
#include "bigton_runtime.h"
#include <stdbool.h>

void bigtonValFree(bigton_tagged_value_t value) {
    switch (value.t) {
        case BIGTON_NULL:
        case BIGTON_INT:
        case BIGTON_FLOAT:
            return;
        case BIGTON_STRING:
            bigtonFreeNullableBuff(value.v.s->content);
            bigtonFreeBuff(value.v.s);
            break;
        case BIGTON_TUPLE: {
            bigton_tuple_t *t = value.v.t;
            for (size_t i = 0; i < t->length; i += 1) {
                bigtonValRcDecr((bigton_tagged_value_t) {
                    .t = t->valueTypes[i], .v = t->values[i]
                });
            }
            bigtonFreeBuff(t->valueTypes);
            bigtonFreeBuff(t->values);
            bigtonFreeBuff(t);
            break;
        }
        case BIGTON_OBJECT: {
            bigton_object_t *o = value.v.o;
            size_t propCount = o->shape->propCount;
            for (size_t i = 0; i < propCount; i += 1) {
                bigtonValRcDecr((bigton_tagged_value_t) {
                    .t = o->memberTypes[i], .v = o->memberValues[i]
                });
            }
            bigtonFreeNullableBuff(o->memberTypes);
            bigtonFreeNullableBuff(o->memberValues);
            bigtonFreeBuff(o);
            break;
        }
        case BIGTON_ARRAY: {
            bigton_array_t *a = value.v.a;
            for (size_t i = 0; i < a->length; i += 1) {
                bigtonValRcDecr((bigton_tagged_value_t) {
                    .t = a->elementTypes[i], .v = a->elementValues[i]
                });
            }
            bigtonFreeNullableBuff(a->elementTypes);
            bigtonFreeNullableBuff(a->elementValues);
            bigtonFreeBuff(a);
            break;
        }
    }
}


bigton_string_t *bigtonAllocConstString(
    bigton_runtime_state_t *r, bigton_str_id_t id
) {
    bigton_parsed_program_t *p = &r->program;
    if (id >= p->numConstStrings) {
        r->error = BIGTONE_INT_INVALID_CONST_STRING;
        return NULL;
    }
    bigton_const_string_t cstr = p->constStrings[id];
    bool isInRange = cstr.firstOffset <= p->numConstStringChars
        && cstr.firstOffset + cstr.charLength <= p->numConstStringChars;
    if (!isInRange) {
        r->error = BIGTONE_INT_INVALID_CONST_STRING;
        return NULL;
    }
    return bigtonAllocString(
        &r->b, cstr.charLength, p->constStringChars + cstr.firstOffset
    );
}

bigton_string_t *bigtonAllocString(
    bigton_buff_owner_t *o, uint64_t length, const bigton_char_t *content
) {
    size_t lengthBytes = sizeof(bigton_char_t) * length;
    bigton_char_t *buffer
        = (bigton_char_t *) bigtonAllocNullableBuff(o, lengthBytes);
    memcpy(buffer, content, lengthBytes);
    bigton_string_t *str
        = (bigton_string_t *) bigtonAllocBuff(o, sizeof(bigton_string_t));
    str->rc = BIGTON_RC_INIT;
    str->length = length;
    str->content = buffer;
    return str;
}