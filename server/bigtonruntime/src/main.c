
/*
 * Main file for testing.
 * TODO! Delete once JNI glue code and JVM integration is complete.
 */

#define BIGTON_ERROR_MACROS
#include <bigton/values.h>
#include <bigton/ir.h>
#include <bigton/runtime.h>
#include <stdio.h>

static void printValue(bigton_tagged_value_t v) {
    bigton_error_t e;
    switch (v.t) {
        case BIGTON_NULL:
            printf("null"); 
            break;
        case BIGTON_INT:
            printf("%li", v.v.i); 
            break;
        case BIGTON_FLOAT:
            printf("%f", v.v.f);
            break;
        case BIGTON_STRING:
            printf("\"");
            fwrite(
                v.v.s->content, sizeof(bigton_char_t),
                v.v.s->length, stdout
            );
            printf("\"");
            break;
        case BIGTON_TUPLE:
            printf("(");
            for (size_t i = 0; i < v.v.t->length; i += 1) {
                if (i > 0) { printf(", "); }
                printValue(bigtonTupleAt(v.v.t, i, &e));
            }
            printf(")");
            break;
        case BIGTON_OBJECT:
            printf("{");
            for (size_t i = 0; i < v.v.o->shape->propCount; i += 1) {
                if (i > 0) { printf(", "); }
                printValue(bigtonObjectAt(v.v.o, i));
            }
            printf("}");
            break;
        case BIGTON_ARRAY:
            printf("[");
            for (size_t i = 0; i < v.v.a->length; i += 1) {
                if (i > 0) { printf(", "); }
                printValue(bigtonArrayAt(v.v.a, i, &e));
            }
            printf("]");
            break;
    }
}

void bigtonBuiltinPrint(bigton_runtime_state_t *r) {
    bigton_tagged_value_t line = bigtonStackPop(&r->stack, r);
    if (HAS_ERROR(r)) { return; }
    printValue(line);
    printf("\n");
}

int main(void) {
    
    bigton_instr_type_t instrTypes[] = {
        // GLOBAL
        BIGTONIR_LOAD_INT,
        BIGTONIR_LOAD_INT,
        BIGTONIR_LOAD_INT,
        BIGTONIR_LOAD_TUPLE,
        BIGTONIR_CALL,
        BIGTONIR_CALL_BUILTIN,
        BIGTONIR_LOAD_INT,
        BIGTONIR_LOAD_INT,
        BIGTONIR_LOAD_INT,
        BIGTONIR_LOAD_ARRAY,
        BIGTONIR_LOAD_INT,
        BIGTONIR_LOAD_ARRAY_ELEMENT,
        BIGTONIR_CALL_BUILTIN,
        
        // 'add'
        BIGTONIR_PUSH_LOCAL,
        BIGTONIR_LOAD_LOCAL,
        BIGTONIR_LOAD_TUPLE_MEMBER,
        BIGTONIR_LOAD_LOCAL,
        BIGTONIR_LOAD_TUPLE_MEMBER,
        BIGTONIR_LOAD_LOCAL,
        BIGTONIR_LOAD_TUPLE_MEMBER,
        BIGTONIR_ADD,
        BIGTONIR_ADD,
        BIGTONIR_RETURN
    };
    bigton_instr_args_t instrArgs[] = {
        // print(add((5, 25, 3)))
        // print([1, 2, 3][0])
        (bigton_instr_args_t) { .loadInt = 5 },
        (bigton_instr_args_t) { .loadInt = 25 },
        (bigton_instr_args_t) { .loadInt = 3 },
        (bigton_instr_args_t) { .loadTupleLength = 3 },
        (bigton_instr_args_t) { .called = 0 },
        (bigton_instr_args_t) { .calledBuiltin = 0 },
        (bigton_instr_args_t) { .loadInt = 25 },
        (bigton_instr_args_t) { .loadInt = 10 },
        (bigton_instr_args_t) { .loadInt = 15 },
        (bigton_instr_args_t) { .loadArrayLength = 3 },
        (bigton_instr_args_t) { .loadInt = 0 },
        (bigton_instr_args_t) {},
        (bigton_instr_args_t) { .calledBuiltin = 0 },
        
        // function add(t) {
        //     return t.0 + t.1 + t.2
        // }
        (bigton_instr_args_t) {},
        (bigton_instr_args_t) { .loadLocal = 0 },
        (bigton_instr_args_t) { .loadTupleMemIdx = 0 },
        (bigton_instr_args_t) { .loadLocal = 0 },
        (bigton_instr_args_t) { .loadTupleMemIdx = 1 },
        (bigton_instr_args_t) { .loadLocal = 0 },
        (bigton_instr_args_t) { .loadTupleMemIdx = 2 },
        (bigton_instr_args_t) {},
        (bigton_instr_args_t) {},
        (bigton_instr_args_t) {}
    };
    size_t numInstrs = sizeof(instrTypes) / sizeof(bigton_instr_type_t);
    
    bigton_const_string_t constStrings[] = {
        (bigton_const_string_t) { .charLength = 9, .firstOffset = 0 },
        (bigton_const_string_t) { .charLength = 3, .firstOffset = 9 },
        (bigton_const_string_t) { .charLength = 5, .firstOffset = 12 }
    };
    bigton_char_t constStringChars[] =
        u"<unknown>"
        u"add"
        u"print";
        
    bigton_shape_t shapes[] = {};
    bigton_shape_prop_t props[] = {};
    
    bigton_function_t functions[] = {
        (bigton_function_t) {
            .name = 1,
            .declSource = {},
            .start = 13,
            .length = 10
        }
    };
    bigton_builtin_function_t builtinFunctions[] = {
        (bigton_builtin_function_t) {
            .name = 2,
            .cost = 1
        }
    };
    void (*bultinFunctionImpls[])(bigton_runtime_state_t *) = {
        &bigtonBuiltinPrint
    };
    
    size_t globalEnd = sizeof(functions) == 0 ? numInstrs
        : functions[0].start;
    
    bigton_parsed_program_t program = {
        .unknownStrId = 0,
        
        .numInstrs = numInstrs,
        .instrTypes = instrTypes,
        .instrArgs = instrArgs,
        
        .numConstStrings = sizeof(constStrings) / sizeof(bigton_const_string_t),
        .constStrings = constStrings,
        .numConstStringChars = sizeof(constStringChars) / sizeof(bigton_char_t),
        .constStringChars = constStringChars,
        
        .numShapes = sizeof(shapes) / sizeof(bigton_shape_t),
        .shapes = shapes,
        .numProps = sizeof(props) / sizeof(bigton_shape_prop_t),
        .props = props,
        
        .numFunctions = sizeof(functions) / sizeof(bigton_function_t),
        .functions = functions,
        .numBuiltinFunctions
            = sizeof(builtinFunctions) / sizeof(bigton_builtin_function_t),
        .builtinFunctions = builtinFunctions,
        
        .numGlobals = 0,
        .globalStart = 0,
        .globalEnd = globalEnd
    };
    
    bigton_runtime_settings_t settings = {
        .tickInstructionLimit = 10000,
        .memoryUsageLimit = 8 * 1024 * 1024,
        .maxCallDepth = 256,
        .maxTupleSize = 8
    };
    bigton_runtime_state_t r;
    bigtonInit(&r, &settings, &program);
    
    bigtonStartTick(&r);
    for (;;) {
        bigton_exec_status_t status = bigtonExecBatch(&r);
        switch (status) {
            case BIGTONST_CONTINUE:
                continue;
            case BIGTONST_EXEC_BUILTIN_FUN:
                bultinFunctionImpls[r.awaitingBuiltinFun](&r);
                continue;
            case BIGTONST_AWAIT_TICK:
                printf("end of tick\n");
                break;
            case BIGTONST_COMPLETE:
                printf("end of program\n");
                break;
            case BIGTONST_ERROR:
                printf("error: %u\n", r.error);
                break;
        }
        break;
    }
    
    for (size_t i = 0; i < r.stack.count; i += 1) {
        printf("stack[%zu]: ", i);
        printValue(bigtonStackAt(&r.stack, i, &r));
        printf("\n");
    }
    
    bigtonFree(&r);
    
}