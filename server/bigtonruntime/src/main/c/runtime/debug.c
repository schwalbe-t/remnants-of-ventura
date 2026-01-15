
#include <bigton/values.h>
#include <bigton/ir.h>
#include <bigton/runtime.h>
#include <stdio.h>
#include <inttypes.h>

static void bigtonDebugPrintStr(
    bigton_parsed_program_t *p, bigton_str_id_t i
) {
    putchar('\'');
    bigton_const_string_t string = p->constStrings[i];
    const bigton_char_t *chars = p->constStringChars + string.firstOffset;
    for (size_t ci = 0; ci < string.charLength; ci += 1) {
        bigton_char_t c = chars[ci];
        putchar(c <= 0x7F ? (char) c : '?');
    }
    putchar('\'');
}

static void bigtonDebugPrintInstr(
    bigton_instr_type_t t, bigton_instr_args_t a
) {
    switch (t) {
        case BIGTONIR_SOURCE_LINE:
            printf("SOURCE_LINE %" PRIu32, a.sourceLine);
            break;
        case BIGTONIR_SOURCE_FILE:
            printf("SOURCE_FILE strId=%" PRIu32, a.sourceLine);
            break;
        case BIGTONIR_DISCARD: printf("DISCARD"); break;
        case BIGTONIR_LOAD_NULL: printf("LOAD_NULL"); break;
        case BIGTONIR_LOAD_INT:
            printf("LOAD_INT %" PRIi64, a.loadInt);
            break;
        case BIGTONIR_LOAD_FLOAT:
            printf("LOAD_FLOAT %f", a.loadFloat);
            break;
        case BIGTONIR_LOAD_STRING:
            printf("LOAD_STRING strId=%" PRIu32, a.loadString);
            break;
        case BIGTONIR_LOAD_TUPLE:
            printf("LOAD_TUPLE len=%" PRIu32, a.loadTupleLength);
            break;
        case BIGTONIR_LOAD_OBJECT:
            printf("LOAD_OBJECT shapeId=%" PRIu32, a.loadObject);
            break;
        case BIGTONIR_LOAD_ARRAY:
            printf("LOAD_ARRAY len=%" PRIu32, a.loadArrayLength);
            break;
        case BIGTONIR_LOAD_TUPLE_MEMBER:
            printf("LOAD_TUPLE_MEMBER idx=%" PRIu32, a.loadTupleMemIdx);
            break;
        case BIGTONIR_LOAD_OBJECT_MEMBER:
            printf("LOAD_OBJECT_MEMBER memStrId=%" PRIu32, a.loadObjectMemName);
            break;
        case BIGTONIR_LOAD_ARRAY_ELEMENT: printf("LOAD_ARRAY_ELEMENT"); break;
        case BIGTONIR_LOAD_GLOBAL:
            printf("LOAD_GLOBAL id=%" PRIu32, a.loadGlobal);
            break;
        case BIGTONIR_LOAD_LOCAL:
            printf("LOAD_LOCAL id=%" PRIu32, a.loadLocal);
            break;
        case BIGTONIR_ADD: printf("ADD"); break;
        case BIGTONIR_SUBTRACT: printf("SUBTRACT"); break;
        case BIGTONIR_MULTIPLY: printf("MULTIPLY"); break;
        case BIGTONIR_DIVIDE: printf("DIVIDE"); break;
        case BIGTONIR_REMAINDER: printf("REMAINDER"); break;
        case BIGTONIR_NEGATE: printf("NEGATE"); break;
        case BIGTONIR_LESS_THAN: printf("LESS_THAN"); break;
        case BIGTONIR_LESS_THAN_EQUAL: printf("LESS_THAN_EQUAL"); break;
        case BIGTONIR_GREATER_THAN: printf("GREATER_THAN"); break;
        case BIGTONIR_GREATER_THAN_EQUAL: printf("GREATER_THAN_EQUAL"); break;
        case BIGTONIR_EQUAL: printf("EQUAL"); break;
        case BIGTONIR_NOT_EQUAL: printf("NOT_EQUAL"); break;
        case BIGTONIR_AND: printf("AND"); break;
        case BIGTONIR_OR: printf("OR"); break;
        case BIGTONIR_NOT: printf("NOT"); break;
        case BIGTONIR_STORE_GLOBAL:
            printf("STORE_GLOBAL id=%" PRIu32, a.storeGlobal);
            break;
        case BIGTONIR_PUSH_LOCAL: printf("PUSH_LOCAL"); break;
        case BIGTONIR_STORE_LOCAL:
            printf("STORE_LOCAL id=%" PRIu32, a.storeLocal);
            break;
        case BIGTONIR_STORE_OBJECT_MEMBER:
            printf("STORE_OBJECT_MEMBER memStrId=%" PRIu32,
                a.storeObjectMemName
            );
            break;
        case BIGTONIR_STORE_ARRAY_ELEMENT: printf("STORE_ARRAY_ELEMENT"); break;
        case BIGTONIR_IF: {
            bigton_if_args_t ip = a.ifParams;
            printf("IF ifLen=%" PRIu32 " elseLen=%" PRIu32,
                ip.ifBodyLength, ip.elseBodyLength
            );
            break;
        }
        case BIGTONIR_LOOP:
            printf("LOOP len=%" PRIu32, a.infLoopLength);
            break;
        case BIGTONIR_TICK:
            printf("TICK len=%" PRIu32, a.tickLoopLength);
            break;
        case BIGTONIR_CONTINUE: printf("CONTINUE"); break;
        case BIGTONIR_BREAK: printf("BREAK"); break;
        case BIGTONIR_CALL:
            printf("CALL id=%" PRIu32, a.called); 
            break;
        case BIGTONIR_CALL_BUILTIN:
            printf("CALL_BUILTIN id=%" PRIu32, a.calledBuiltin);
            break;
        case BIGTONIR_RETURN: printf("RETURN"); break;
    }
    putchar('\n');
}

void bigtonDebugProgram(bigton_parsed_program_t *p) {
    printf("--- Program Header ---\n");
    printf("unknownStrId        = %" PRIu32 "\n", p->unknownStrId);
    printf("numInstrs           = %" PRIu32 "\n", p->numInstrs);
    printf("numConstStrings     = %" PRIu32 "\n", p->numConstStrings);
    printf("numConstStringChars = %zu\n", p->numConstStringChars);
    printf("numShapes           = %" PRIu32 "\n", p->numShapes);
    printf("numProps            = %zu\n", p->numProps);
    printf("numFunctions        = %" PRIu32 "\n", p->numFunctions);
    printf("numBuiltinFunctions = %" PRIu32 "\n", p->numBuiltinFunctions);
    printf("numGlobals          = %" PRIu32 "\n", p->numGlobals);
    printf("globalStart         = %" PRIu32 "\n", p->globalStart);
    printf("globalEnd           = %" PRIu32 "\n", p->globalEnd);
    printf("\n--- Constant Strings ---\n");
    for (bigton_str_id_t i = 0; i < p->numConstStrings; i += 1) {
        printf("[%" PRIu32 "] ", i);
        bigtonDebugPrintStr(p, i);
        putchar('\n');
    }
    printf("\n--- Object Shapes ---\n");
    for (bigton_shape_id_t i = 0; i < p->numShapes; i += 1) {
        printf("[%" PRIu32 "] ", i);
        bigton_shape_t shape = p->shapes[i];
        const bigton_shape_prop_t *props = p->props + shape.firstPropOffset;
        for (size_t pi = 0; pi < shape.propCount; pi += 1) {
            if (pi >= 1) { printf(", "); }
            bigtonDebugPrintStr(p, props[pi].name);
        }
        printf("\n");
    }
    printf("\n--- Builtin Functions ---\n");
    for (bigton_slot_t i = 0; i < p->numBuiltinFunctions; i += 1) {
        printf("[%" PRIu32 "] ", i);
        bigton_builtin_function_t f = p->builtinFunctions[i];
        bigtonDebugPrintStr(p, f.name);
        printf(": cost = %" PRIu32 "\n", f.cost);
    }
    printf("\n--- Functions ---\n");
    for (bigton_slot_t i = 0; i < p->numFunctions; i += 1) {
        printf("[%" PRIu32 "] ", i);
        bigton_function_t f = p->functions[i];
        bigtonDebugPrintStr(p, f.name);
        printf(": declFile = ");
        bigtonDebugPrintStr(p, f.declSource.file);
        printf(", declLine = %" PRIu32 "\n", f.declSource.line);
        for (bigton_instr_idx_t ii = 0; ii < f.length; ii += 1) {
            bigton_instr_idx_t aii = f.start + ii;
            bigtonDebugPrintInstr(p->instrTypes[aii], p->instrArgs[aii]);
        }
    }
    printf("\n--- Global ---\n");
    for (bigton_instr_idx_t i = p->globalStart; i < p->globalEnd; i += 1) {
        bigtonDebugPrintInstr(p->instrTypes[i], p->instrArgs[i]);
    }
    putchar('\n');
    fflush(stdout);
}