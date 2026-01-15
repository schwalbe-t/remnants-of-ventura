
#define BIGTON_ERROR_MACROS
#include <bigton/values.h>
#include <bigton/ir.h>
#include <bigton/error.h>
#include <bigton/runtime.h>
#include <stdbool.h>
#include <string.h>
#include <math.h>

void bigtonLogLine(bigton_runtime_state_t *r, bigton_string_t *line) {
    size_t oldCount = r->logsCount;
    if (oldCount >= r->logsCapacity) {
        r->logsCapacity += 8;
        r->logs = bigtonReallocNullableBuff(
            &r->b, r->logs, sizeof(bigton_string_t *) * r->logsCapacity
        );
    }
    line->rc.count += 1;
    r->logs[oldCount] = line;
    r->logsCount = oldCount + 1;
}

void bigtonTracePush(bigton_runtime_state_t *r, bigton_trace_call_t t) {
    size_t oldCount = r->traceCount;
    if (oldCount >= r->traceCapacity) {
        r->traceCapacity += 8;
        r->trace = bigtonReallocNullableBuff(
            &r->b, r->trace, sizeof(bigton_trace_call_t) * r->traceCapacity
        );
    }
    r->trace[oldCount] = t;
    r->traceCount += 1;
}

void bigtonTracePop(bigton_runtime_state_t *r) {
    size_t oldCount = r->traceCount;
    if (oldCount == 0) { return; }
    r->traceCount = oldCount - 1;
}

void bigtonStackPush(
    bigton_value_stack_t *s, bigton_tagged_value_t value,
    bigton_runtime_state_t *r
) {
    size_t oldCount = s->count;
    if (oldCount >= s->capacity) {
        s->capacity += 16;
        s->types = bigtonReallocNullableBuff(
            &r->b, s->types, sizeof(bigton_value_type_t) * s->capacity
        );
        s->values = bigtonReallocNullableBuff(
            &r->b, s->values, sizeof(bigton_value_t) * s->capacity
        );
    }
    s->types[oldCount] = value.t;
    s->values[oldCount] = value.v;
    s->count = oldCount + 1;
}

bigton_tagged_value_t bigtonStackSet(
    bigton_value_stack_t *s, size_t i, bigton_tagged_value_t v,
    bigton_runtime_state_t *r
) {
    if (i >= s->count) {
        r->error = BIGTONE_INT_STACK_IDX_OOB;
        return BIGTON_NULL_VALUE;
    }
    bigton_tagged_value_t oldValue = {
        .t = s->types[i], .v = s->values[i]
    };
    s->types[i] = v.t;
    s->values[i] = v.v;
    return oldValue;
}

bigton_tagged_value_t bigtonStackAt(
    bigton_value_stack_t *s, size_t i, bigton_runtime_state_t *r
) {
    if (i >= s->count) {
        r->error = BIGTONE_INT_STACK_IDX_OOB;
        return BIGTON_NULL_VALUE;
    }
    return (bigton_tagged_value_t) {
        .t = s->types[i], .v = s->values[i]
    };
}

bigton_tagged_value_t bigtonStackPop(
    bigton_value_stack_t *s, bigton_runtime_state_t *r
) {
    size_t oldCount = s->count;
    if (oldCount == 0) {
        r->error = BIGTONE_INT_STACK_EMPTY;
        return BIGTON_NULL_VALUE;
    }
    size_t newCount = oldCount - 1;
    s->count = newCount;
    bigton_value_type_t type = s->types[newCount];
    bigton_value_t value = s->values[newCount];
    return (bigton_tagged_value_t) { .t = type, .v = value };
}

void bigtonScopePush(bigton_runtime_state_t *r, bigton_scope_t scope) {
    size_t oldCount = r->scopesCount;
    if (oldCount >= r->scopesCapacity) {
        r->scopesCapacity += 8;
        r->scopes = bigtonReallocNullableBuff(
            &r->b, r->scopes, sizeof(bigton_scope_t) * r->scopesCapacity
        );
    }
    r->scopes[oldCount] = scope;
    r->scopesCount = oldCount + 1;
}

bigton_scope_t *bigtonScopeCurr(bigton_runtime_state_t *r) {
    size_t count = r->scopesCount;
    if (count == 0) {
        r->error = BIGTONE_INT_SCOPE_STACK_EMPTY;
        return NULL;
    }
    return r->scopes + count - 1;
}

void bigtonScopePop(bigton_runtime_state_t *r) {
    size_t oldCount = r->scopesCount;
    if (oldCount == 0) {
        r->error = BIGTONE_INT_SCOPE_STACK_EMPTY;
        return;
    }
    size_t newCount = oldCount - 1;
    r->scopesCount = newCount;
    bigton_scope_t scope = r->scopes[newCount];
    for (size_t i = 0; i < scope.numLocals; i += 1) {
        bigtonValRcDecr(bigtonStackPop(&r->locals, r));
    }
}

static void assertGlobalVarValid(bigton_runtime_state_t *r, bigton_slot_t id) {
    if (id < r->program.numGlobals) { return; }
    r->error = BIGTONE_INT_GLOBAL_VAR_REF_INVALID;
}

#define ARITHMETIC_BIOP_INSTR(iop, fop, dbz) \
    bigton_tagged_value_t bv = bigtonStackPop(&r->stack, r); \
    bigton_tagged_value_t av = bigtonStackPop(&r->stack, r); \
    if (av.t == BIGTON_INT && bv.t == BIGTON_INT) { \
        uint64_t a = (uint64_t) av.v.i; \
        uint64_t b = (uint64_t) bv.v.i; \
        if (!(dbz)) { \
            uint64_t c = iop; \
            bigtonStackPush(&r->stack, BIGTON_INT_VALUE((bigton_int_t) c), r); \
        } else if (!HAS_ERROR(r)) { \
            r->error = BIGTONE_INT_DIVISION_BY_ZERO; \
        } \
    } else if (av.t == BIGTON_FLOAT && bv.t == BIGTON_FLOAT) { \
        double a = av.v.f; \
        double b = bv.v.f; \
        bigtonStackPush(&r->stack, BIGTON_FLOAT_VALUE(fop), r); \
    } else if (!HAS_ERROR(r)) { \
        r->error = BIGTONE_OPERANDS_NOT_NUMBERS; \
    } \
    bigtonValRcDecr(av); \
    bigtonValRcDecr(bv);
    
#define COMPARISON_BIOP_INSTR(iop, fop) \
    bigton_tagged_value_t bv = bigtonStackPop(&r->stack, r); \
    bigton_tagged_value_t av = bigtonStackPop(&r->stack, r); \
    if (av.t == BIGTON_INT && bv.t == BIGTON_INT) { \
        bigton_int_t a = av.v.i; \
        bigton_int_t b = bv.v.i; \
        bigtonStackPush(&r->stack, BIGTON_INT_VALUE(iop), r); \
    } else if (av.t == BIGTON_FLOAT && bv.t == BIGTON_FLOAT) { \
        double a = av.v.f; \
        double b = bv.v.f; \
        bigtonStackPush(&r->stack, BIGTON_INT_VALUE(fop), r); \
    } else if (!HAS_ERROR(r)) { \
        r->error = BIGTONE_OPERANDS_NOT_NUMBERS; \
    } \
    bigtonValRcDecr(av); \
    bigtonValRcDecr(bv);
    
bool valuesEqual(bigton_tagged_value_t a, bigton_tagged_value_t b) {
    if (a.t != b.t) { return false; }
    switch (a.t) {
        case BIGTON_NULL: return true;
        case BIGTON_INT: return a.v.i == b.v.i;
        case BIGTON_FLOAT: return a.v.f == b.v.f;
        case BIGTON_STRING: {
            if (a.v.s->length != b.v.s->length) { return false; }
            size_t lengthBytes = sizeof(bigton_char_t) * a.v.s->length;
            return memcmp(a.v.s->content, b.v.s->content, lengthBytes) == 0;
        }
        case BIGTON_TUPLE: {
            if (a.v.t->length != b.v.t->length) { return false; }
            if (a.v.t->flatLength != b.v.t->flatLength) { return false; }
            size_t l = a.v.t->length;
            for (size_t i = 0; i < l; i += 1) {
                bigton_tagged_value_t am = {
                    .t = a.v.t->valueTypes[i],
                    .v = a.v.t->values[i]
                };
                bigton_tagged_value_t bm = {
                    .t = b.v.t->valueTypes[i],
                    .v = b.v.t->values[i]
                };
                if (!valuesEqual(am, bm)) { return false; }
            }
            return true;
        }
        case BIGTON_OBJECT: return a.v.o == b.v.o;
        case BIGTON_ARRAY: return a.v.a == b.v.a;
    }
}

bool valueIsTruthy(bigton_tagged_value_t x) {
    switch (x.t) {
        case BIGTON_NULL:
            return false;
        case BIGTON_INT:
            return x.v.i != 0;
        case BIGTON_FLOAT:
            return x.v.f != 0.0 && !isnan(x.v.f);
        case BIGTON_STRING:
        case BIGTON_TUPLE:
        case BIGTON_OBJECT:
        case BIGTON_ARRAY:
            return true;
    }
}

#include <stdio.h>

bigton_exec_status_t bigtonExecInstr(bigton_runtime_state_t *r) {
    if (r->b.totalSizeBytes > r->settings.memoryUsageLimit) {
        r->error = BIGTONE_EXCEEDED_MEMORY_LIMIT;
        return BIGTONST_ERROR;
    }
    if (r->accCost > r->settings.tickInstructionLimit) {
        r->error = BIGTONE_EXCEEDED_INSTR_LIMIT;
        return BIGTONST_ERROR;
    }
    bigton_scope_t *scope = bigtonScopeCurr(r);
    if (HAS_ERROR(r)) { return BIGTONST_ERROR; }
    bigton_instr_idx_t instrIdx = r->currentInstr;
    if (instrIdx >= scope->end) {
        switch (scope->type) {
            case BIGTONSC_GLOBAL:
                bigtonScopePop(r);
                return BIGTONST_COMPLETE;
            case BIGTONSC_FUNCTION:
                r->currentInstr = scope->after;
                bigtonScopePop(r);
                bigtonTracePop(r);
                bigtonStackPush(&r->stack, BIGTON_NULL_VALUE, r);
                return BIGTONST_COMPLETE;
            case BIGTONSC_LOOP:
                r->currentInstr = scope->start;
                return BIGTONST_CONTINUE;
            case BIGTONSC_TICK:
                r->currentInstr = scope->start;
                return BIGTONST_AWAIT_TICK;
            case BIGTONSC_IF:
                r->currentInstr = scope->after;
                return BIGTONST_CONTINUE;
        }
    }
    if (instrIdx >= r->program.numInstrs) {
        r->error = BIGTONE_INT_INSTR_COUNTER_OOB;
        return BIGTONST_ERROR;
    }
    bigton_instr_type_t instrType = r->program.instrTypes[instrIdx];
    bigton_instr_args_t instrArgs = r->program.instrArgs[instrIdx];
    switch (instrType) {
        case BIGTONIR_SOURCE_LINE: {
            r->currentSource.line = instrArgs.sourceLine;
            break;
        }
        case BIGTONIR_SOURCE_FILE: {
            r->currentSource.file = instrArgs.sourceFile;
            break;
        }
        case BIGTONIR_DISCARD: {
            bigtonValRcDecr(bigtonStackPop(&r->stack, r));
            break;
        }
        
        case BIGTONIR_LOAD_NULL: {
            bigtonStackPush(&r->stack, BIGTON_NULL_VALUE, r);
            break;
        }
        case BIGTONIR_LOAD_INT: {
            bigtonStackPush(
                &r->stack, BIGTON_INT_VALUE(instrArgs.loadInt), r
            );
            break;
        }
        case BIGTONIR_LOAD_FLOAT: {
            bigtonStackPush(
                &r->stack, BIGTON_FLOAT_VALUE(instrArgs.loadFloat), r
            );
            break;
        }
        case BIGTONIR_LOAD_STRING: {
            bigtonStackPush(
                &r->stack,
                BIGTON_STRING_VALUE(
                    bigtonAllocConstString(r, instrArgs.loadString)
                ), 
                r
            );
            break;
        }
        case BIGTONIR_LOAD_TUPLE: {
            size_t length = instrArgs.loadTupleLength;
            bigton_value_type_t *memberTypes
                = bigtonAllocBuff(&r->b, sizeof(bigton_value_type_t) * length);
            bigton_value_t *memberValues
                = bigtonAllocBuff(&r->b, sizeof(bigton_value_t) * length);
            size_t flatLength = 0;
            for (int64_t i = length - 1; i >= 0; i -= 1) {
                bigton_tagged_value_t mv = bigtonStackPop(&r->stack, r);
                memberTypes[i] = mv.t;
                memberValues[i] = mv.v;
                flatLength += mv.t != BIGTON_TUPLE ? 1 : mv.v.t->flatLength;
            }
            if (!HAS_ERROR(r) && flatLength > r->settings.maxTupleSize) {
                r->error = BIGTONE_TUPLE_TOO_BIG;
            }
            bigton_tuple_t *tuple
                = bigtonAllocBuff(&r->b, sizeof(bigton_tuple_t));
            tuple->rc = BIGTON_RC_INIT;
            tuple->length = (uint32_t) length;
            tuple->flatLength = (uint32_t) flatLength;
            tuple->valueTypes = memberTypes;
            tuple->values = memberValues;
            bigtonStackPush(&r->stack, BIGTON_TUPLE_VALUE(tuple), r);
            break;
        }
        case BIGTONIR_LOAD_OBJECT: {
            bigton_shape_id_t shapeId = instrArgs.loadObject;
            if (shapeId >= r->program.numShapes) {
                r->error = BIGTONE_INT_SHAPE_ID_OOB;
                return BIGTONST_ERROR;
            }
            bigton_shape_t *shape = &r->program.shapes[shapeId];
            size_t numAllProps = r->program.numProps;
            bool propsInBounds = shape->firstPropOffset < numAllProps
                && shape->firstPropOffset + shape->propCount < numAllProps;
            if (!propsInBounds) {
                r->error = BIGTONE_INT_SHAPE_PROP_IDX_OOB;
                return BIGTONST_ERROR;
            }
            const bigton_shape_prop_t *props
                = r->program.props + shape->firstPropOffset;
            size_t propCount = shape->propCount;
            bigton_value_type_t *memberTypes = bigtonAllocNullableBuff(
                &r->b, sizeof(bigton_value_type_t) * propCount
            );
            bigton_value_t *memberValues = bigtonAllocNullableBuff(
                &r->b, sizeof(bigton_value_t) * propCount
            );
            for (int64_t i = (int64_t) propCount - 1; i >= 0; i -= 1) {
                bigton_tagged_value_t mv = bigtonStackPop(&r->stack, r);
                memberTypes[i] = mv.t;
                memberValues[i] = mv.v;
            }
            bigton_object_t *o
                = bigtonAllocBuff(&r->b, sizeof(bigton_object_t));
            o->rc = BIGTON_RC_INIT;
            o->shape = shape;
            o->memberTypes = memberTypes;
            o->memberValues = memberValues;
            bigtonStackPush(&r->stack, BIGTON_OBJECT_VALUE(o), r);
            break;
        }
        case BIGTONIR_LOAD_ARRAY: {
            size_t length = instrArgs.loadArrayLength;
            bigton_value_type_t *elementTypes = bigtonAllocNullableBuff(
                &r->b, sizeof(bigton_value_type_t) * length
            );
            bigton_value_t *elementValues = bigtonAllocNullableBuff(
                &r->b, sizeof(bigton_value_t) * length
            );
            for (int64_t i = length - 1; i >= 0; i -= 1) {
                bigton_tagged_value_t ev = bigtonStackPop(&r->stack, r);
                elementTypes[i] = ev.t;
                elementValues[i] = ev.v;
            }
            bigton_array_t *array
                = bigtonAllocBuff(&r->b, sizeof(bigton_array_t));
            array->rc = BIGTON_RC_INIT;
            array->capacity = (uint32_t) length;
            array->length = (uint32_t) length;
            array->elementTypes = elementTypes;
            array->elementValues = elementValues;
            bigtonStackPush(&r->stack, BIGTON_ARRAY_VALUE(array), r);
            break;
        }
            
        case BIGTONIR_LOAD_TUPLE_MEMBER: {
            bigton_tagged_value_t t = bigtonStackPop(&r->stack, r);
            if (t.t == BIGTON_TUPLE) {
                uint32_t i = instrArgs.loadTupleMemIdx;
                bigton_tagged_value_t mv = bigtonTupleAt(t.v.t, i, &r->error);
                bigtonValRcIncr(mv);
                bigtonStackPush(&r->stack, mv, r);
            } else if (!HAS_ERROR(r)) {
                r->error = BIGTONE_OPERAND_NOT_TUPLE;
            }
            bigtonValRcDecr(t);
            break;
        }
        case BIGTONIR_LOAD_OBJECT_MEMBER: {
            bigton_tagged_value_t o = bigtonStackPop(&r->stack, r);
            if (o.t == BIGTON_OBJECT) {
                size_t i = bigtonObjectFindMem(
                    o.v.o, instrArgs.loadObjectMemName, 
                    r->program.props, r->program.numProps, &r->error
                );
                if (!HAS_ERROR(r)) {
                    bigton_tagged_value_t mem = bigtonObjectAt(o.v.o, i);
                    bigtonValRcIncr(mem);
                    bigtonStackPush(&r->stack, mem, r);
                }
            } else if (!HAS_ERROR(r)) {
                r->error = BIGTONE_OPERAND_NOT_OBJECT;
            }
            bigtonValRcDecr(o);
            break;
        }
        case BIGTONIR_LOAD_ARRAY_ELEMENT: {
            bigton_tagged_value_t iv = bigtonStackPop(&r->stack, r);
            bigton_tagged_value_t av = bigtonStackPop(&r->stack, r);
            if (av.t == BIGTON_ARRAY) {
                if (iv.t == BIGTON_INT) {
                    bigton_tagged_value_t ev
                        = bigtonArrayAt(av.v.a, iv.v.i, &r->error);
                    bigtonValRcIncr(ev);
                    bigtonStackPush(&r->stack, ev, r);
                } else if (!HAS_ERROR(r)) {
                    r->error = BIGTONE_OPERAND_NOT_INTEGER;
                }
            } else if (!HAS_ERROR(r)) {
                r->error = BIGTONE_OPERAND_NOT_ARRAY;
            }
            bigtonValRcDecr(av);
            bigtonValRcDecr(iv);
            break;
        }
        case BIGTONIR_LOAD_GLOBAL: {
            bigton_slot_t globalId = instrArgs.loadGlobal;
            assertGlobalVarValid(r, globalId);
            if (HAS_ERROR(r)) { return BIGTONST_ERROR; }
            bigton_tagged_value_t value = {
                .t = r->globalTypes[instrArgs.loadGlobal],
                .v = r->globalValues[instrArgs.loadGlobal]
            };
            bigtonValRcIncr(value);
            bigtonStackPush(&r->stack, value, r);
            break;
        }
        case BIGTONIR_LOAD_LOCAL: {
            size_t relIdx = instrArgs.loadLocal;
            size_t localCount = r->locals.count;
            if (relIdx >= r->locals.count) {
                r->error = BIGTONE_INT_LOCAL_IDX_OOB;
                return BIGTONST_ERROR;
            }
            size_t idx = localCount - 1 - relIdx; 
            bigton_tagged_value_t value = bigtonStackAt(
                &r->locals, idx, r
            );
            bigtonValRcIncr(value);
            bigtonStackPush(&r->stack, value, r);
            break;
        }
        
        case BIGTONIR_ADD: {
            ARITHMETIC_BIOP_INSTR(a + b, a + b, false)
            break;
        }
        case BIGTONIR_SUBTRACT: {
            ARITHMETIC_BIOP_INSTR(a - b, a - b, false)
            break;
        }
        case BIGTONIR_MULTIPLY: {
            ARITHMETIC_BIOP_INSTR(a * b, a * b, false)
            break;
        }
        case BIGTONIR_DIVIDE: {
            ARITHMETIC_BIOP_INSTR(a / b, a / b, b == 0)
            break;
        }
        case BIGTONIR_REMAINDER: {
            ARITHMETIC_BIOP_INSTR(a % b, fmod(a, b), b == 0)
            break;
        }
        case BIGTONIR_NEGATE: {
            bigton_tagged_value_t xv = bigtonStackPop(&r->stack, r);
            if (xv.t == BIGTON_INT) {
                uint64_t x = (uint64_t) xv.v.i;
                bigton_int_t result = (bigton_int_t) -x;
                bigtonStackPush(&r->stack, BIGTON_INT_VALUE(result), r);
            } else if (xv.t == BIGTON_FLOAT) {
                bigtonStackPush(&r->stack, BIGTON_FLOAT_VALUE(-xv.v.f), r);
            } else if (!HAS_ERROR(r)) {
                r->error = BIGTONE_OPERANDS_NOT_NUMBERS;
            }
            bigtonValRcDecr(xv);
            break;
        }
        
        case BIGTONIR_LESS_THAN: {
            COMPARISON_BIOP_INSTR(a < b, a < b)
            break;
        }
        case BIGTONIR_LESS_THAN_EQUAL: {
            COMPARISON_BIOP_INSTR(a <= b, a <= b)
            break;
        }
        case BIGTONIR_GREATER_THAN: {
            COMPARISON_BIOP_INSTR(a > b, a > b)
            break;
        }
        case BIGTONIR_GREATER_THAN_EQUAL: {
            COMPARISON_BIOP_INSTR(a >= b, a >= b)
            break;
        }
        case BIGTONIR_EQUAL: {
            bigton_tagged_value_t b = bigtonStackPop(&r->stack, r);
            bigton_tagged_value_t a = bigtonStackPop(&r->stack, r);
            bigtonStackPush(&r->stack, BIGTON_INT_VALUE(valuesEqual(a, b)), r);
            bigtonValRcDecr(a);
            bigtonValRcDecr(b);
            break;
        }
        case BIGTONIR_NOT_EQUAL: {
            bigton_tagged_value_t b = bigtonStackPop(&r->stack, r);
            bigton_tagged_value_t a = bigtonStackPop(&r->stack, r);
            bigtonStackPush(&r->stack, BIGTON_INT_VALUE(!valuesEqual(a, b)), r);
            bigtonValRcDecr(a);
            bigtonValRcDecr(b);
            break;
        }
        
        case BIGTONIR_AND: {
            bigton_tagged_value_t b = bigtonStackPop(&r->stack, r);
            bigton_tagged_value_t a = bigtonStackPop(&r->stack, r);
            if (!valueIsTruthy(a)) {
                bigtonStackPush(&r->stack, a, r);
                bigtonValRcIncr(a);
            } else {
                bigtonStackPush(&r->stack, b, r);
                bigtonValRcIncr(b);
            }
            bigtonValRcDecr(a);
            bigtonValRcDecr(b);
            break;
        }
        case BIGTONIR_OR: {
            bigton_tagged_value_t b = bigtonStackPop(&r->stack, r);
            bigton_tagged_value_t a = bigtonStackPop(&r->stack, r);
            if (valueIsTruthy(a)) {
                bigtonStackPush(&r->stack, a, r);
                bigtonValRcIncr(a);
            } else {
                bigtonStackPush(&r->stack, b, r);
                bigtonValRcIncr(b);
            }
            bigtonValRcDecr(a);
            bigtonValRcDecr(b);
            break;
        }
        case BIGTONIR_NOT: {
            bigton_tagged_value_t x = bigtonStackPop(&r->stack, r);
            bigtonStackPush(&r->stack, BIGTON_INT_VALUE(!valueIsTruthy(x)), r);
            bigtonValRcDecr(x);
            break;
        }
        
        case BIGTONIR_STORE_GLOBAL: {
            bigton_slot_t globalId = instrArgs.storeGlobal;
            assertGlobalVarValid(r, globalId);
            if (HAS_ERROR(r)) { return BIGTONST_ERROR; }
            bigton_value_type_t *globalType = r->globalTypes + globalId;
            bigton_value_t *globalValue = r->globalValues + globalId;
            bigton_tagged_value_t value = bigtonStackPop(&r->stack, r);
            bigtonValRcDecr((bigton_tagged_value_t) {
                .t = *globalType, .v = *globalValue
            });
            *globalType = value.t;
            *globalValue = value.v;
            break;
        }
        case BIGTONIR_PUSH_LOCAL: {
            bigton_tagged_value_t value = bigtonStackPop(&r->stack, r);
            bigtonStackPush(&r->locals, value, r);
            scope->numLocals += 1;
            break;
        }
        case BIGTONIR_STORE_LOCAL: {
            bigton_tagged_value_t value = bigtonStackPop(&r->stack, r);
            size_t relIdx = instrArgs.storeLocal;
            size_t localCount = r->locals.count;
            if (relIdx >= r->locals.count) {
                r->error = BIGTONE_INT_LOCAL_IDX_OOB;
                return BIGTONST_ERROR;
            }
            size_t idx = localCount - 1 - relIdx; 
            bigton_tagged_value_t oldValue = bigtonStackSet(
                &r->stack, idx, value, r
            );
            bigtonValRcDecr(oldValue);
            break;
        }
        case BIGTONIR_STORE_OBJECT_MEMBER: {
            bigton_tagged_value_t v = bigtonStackPop(&r->stack, r);
            bigton_tagged_value_t o = bigtonStackPop(&r->stack, r);
            if (o.t == BIGTON_OBJECT) {
                size_t i = bigtonObjectFindMem(
                    o.v.o, instrArgs.storeObjectMemName, 
                    r->program.props, r->program.numProps, &r->error
                );
                if (!HAS_ERROR(r)) {
                    bigton_tagged_value_t oldMv = bigtonObjectSet(o.v.o, i, v);
                    bigtonValRcDecr(oldMv);
                }
            } else if (!HAS_ERROR(r)) {
                r->error = BIGTONE_OPERAND_NOT_OBJECT;
            }
            bigtonValRcDecr(o);
            break;
        }
        case BIGTONIR_STORE_ARRAY_ELEMENT: {
            bigton_tagged_value_t v = bigtonStackPop(&r->stack, r);
            bigton_tagged_value_t i = bigtonStackPop(&r->stack, r);
            bigton_tagged_value_t a = bigtonStackPop(&r->stack, r);
            if (a.t == BIGTON_ARRAY) {
                if (i.t == BIGTON_INT) {
                    bigton_tagged_value_t oldEv
                        = bigtonArraySet(a.v.a, i.v.i, v, &r->error);
                    bigtonValRcDecr(oldEv);
                } else if (!HAS_ERROR(r)) {
                    r->error = BIGTONE_OPERAND_NOT_INTEGER;
                }
            } else if (!HAS_ERROR(r)) {
                r->error = BIGTONE_OPERAND_NOT_ARRAY;
            }
            bigtonValRcDecr(a);
            bigtonValRcDecr(i);
            break;
        }
            
        case BIGTONIR_IF: {
            bigton_if_args_t args = instrArgs.ifParams;
            bigton_instr_idx_t elseStart = instrIdx + 1 + args.if_body_length;
            bigton_instr_idx_t elseEnd = elseStart + args.else_body_length;
            bigton_tagged_value_t cond = bigtonStackPop(&r->stack, r);
            bool isTruthy = valueIsTruthy(cond);
            bigtonScopePush(r, (bigton_scope_t) {
                .type = BIGTONSC_IF,
                .start = isTruthy
                    ? instrIdx + 1
                    : elseStart,
                .end = isTruthy
                    ? elseStart
                    : elseEnd,
                .after = elseEnd,
                .numLocals = 0
            });
            break;
        }
        case BIGTONIR_LOOP: {
            bigton_instr_idx_t end = instrIdx + 1 + instrArgs.infLoopLength;
            bigtonScopePush(r, (bigton_scope_t) {
                .type = BIGTONSC_LOOP,
                .start = instrIdx + 1,
                .end = end,
                .after = end,
                .numLocals = 0
            });
            break;
        }
        case BIGTONIR_TICK: {
            bigton_instr_idx_t end = instrIdx + 1 + instrArgs.infLoopLength;
            bigtonScopePush(r, (bigton_scope_t) {
                .type = BIGTONSC_TICK,
                .start = instrIdx + 1,
                .end = end,
                .after = end,
                .numLocals = 0
            });
            break;
        }
        case BIGTONIR_CONTINUE: {
            bigton_scope_t *currScope = scope;
            while (true) {
                switch (currScope->type) {
                    case BIGTONSC_GLOBAL:
                    case BIGTONSC_FUNCTION:
                        break;
                    case BIGTONSC_LOOP:
                    case BIGTONSC_TICK:
                        r->currentInstr = scope->end;
                        return BIGTONST_CONTINUE;
                    case BIGTONSC_IF:
                        bigtonScopePop(r);
                        currScope = bigtonScopeCurr(r);
                        if (HAS_ERROR(r)) { return BIGTONST_ERROR; }
                        continue;
                }
                break;
            }
            break;
        }
        case BIGTONIR_BREAK: {
            bigton_scope_t *currScope = scope;
            while (true) {
                switch (currScope->type) {
                    case BIGTONSC_GLOBAL:
                    case BIGTONSC_FUNCTION:
                        break;
                    case BIGTONSC_LOOP:
                    case BIGTONSC_TICK:
                        bigtonScopePop(r);
                        r->currentInstr = scope->after;
                        return BIGTONST_CONTINUE;
                    case BIGTONSC_IF:
                        bigtonScopePop(r);
                        currScope = bigtonScopeCurr(r);
                        if (HAS_ERROR(r)) { return BIGTONST_ERROR; }
                        continue;
                }
                break;
            }
            break;
        }
        case BIGTONIR_CALL: {
            if (r->traceCount + 1 > r->settings.maxCallDepth) {
                r->error = BIGTONE_EXCEEDED_MAXIMUM_CALL_DEPTH;
                return BIGTONST_ERROR;
            }
            bigton_slot_t funcId = instrArgs.called;
            if (funcId >= r->program.numFunctions) {
                r->error = BIGTONE_INT_FUN_REF_INVALID;
                return BIGTONST_ERROR;
            }
            bigton_function_t *func = r->program.functions + funcId;
            bigtonTracePush(r, (bigton_trace_call_t) {
                .name = func->name,
                .definedAt = func->declSource,
                .calledFrom = r->currentSource
            });
            bigton_instr_idx_t start = func->start;
            bigtonScopePush(r, (bigton_scope_t) {
                .type = BIGTONSC_FUNCTION,
                .start = start,
                .end = start + func->length,
                .after = instrIdx + 1,
                .numLocals = 0
            });
            r->currentInstr = start;
            return BIGTONST_CONTINUE;
        }
        case BIGTONIR_CALL_BUILTIN: {
            bigton_slot_t builtinId = instrArgs.calledBuiltin;
            if (builtinId >= r->program.numBuiltinFunctions) {
                r->error = BIGTONE_INT_BUILTIN_FUN_REF_INVALID;
                return BIGTONST_ERROR;
            }
            bigton_builtin_function_t *builtin
                = r->program.builtinFunctions + builtinId;
            r->accCost += builtin->cost;
            r->currentInstr += 1;
            r->awaitingBuiltinFun = builtinId;
            return BIGTONST_EXEC_BUILTIN_FUN;
        }
        case BIGTONIR_RETURN: {
            bigton_scope_t *currScope = scope;
            while (true) {
                switch (currScope->type) {
                    case BIGTONSC_GLOBAL:
                        break;
                    case BIGTONSC_FUNCTION:
                        r->currentInstr = currScope->after;
                        bigtonScopePop(r);
                        bigtonTracePop(r);
                        return BIGTONST_CONTINUE;
                    case BIGTONSC_LOOP:
                    case BIGTONSC_TICK:
                    case BIGTONSC_IF:
                        bigtonScopePop(r);
                        currScope = bigtonScopeCurr(r);
                        if (HAS_ERROR(r)) { return BIGTONST_ERROR; }
                        continue;
                }
            }
            break;
        }
    }
    r->currentInstr += 1;
    r->accCost += 1;
    return BIGTONST_CONTINUE;
}

void bigtonStartTick(bigton_runtime_state_t *r) {
    r->accCost = 0;
}

bigton_exec_status_t bigtonExecBatch(bigton_runtime_state_t *r) {
    while (true) {
        bigton_exec_status_t status = bigtonExecInstr(r);
        if (HAS_ERROR(r)) {
            return BIGTONST_ERROR;
        }
        if (status != BIGTONST_CONTINUE) {
            return status;
        }
    }
}