
#define BIGTON_ERROR_MACROS
#include "bigton.h"
#include "bigton_ir.h"
#include "bigton_runtime.h"

static void parseProgram(
    bigton_runtime_state_t *r,
    const uint8_t *rawProgram, size_t rawProgramSize
) {
    bigton_parsed_program_t *dest = &r->program;
    const uint8_t *p = rawProgram;
    
    bigton_program_t *header = (bigton_program_t *) rawProgram;
    dest->unknownStrId          = header->unknownStrId;
    dest->numInstrs             = header->numInstrs;
    dest->numConstStrings       = header->numStrings;
    dest->numConstStringChars   = header->numConstStringChars;
    dest->numShapes             = header->numShapes;
    dest->numProps              = header->numShapeProps;
    dest->numFunctions          = header->numFunctions;
    dest->numBuiltinFunctions   = header->numBuiltinFunctions;
    dest->numGlobals            = header->numGlobalVars;
    dest->globalStart           = header->globalStart;
    dest->globalEnd             = header->globalStart + header->globalLength;
    p += sizeof(bigton_program_t);
    
    dest->instrArgs = (bigton_instr_args_t *) p;
    p += sizeof(bigton_instr_args_t) * header->numInstrs;
    
    dest->constStrings = (bigton_const_string_t *) p;
    p += sizeof(bigton_const_string_t) * header->numStrings;
    
    dest->shapes = (bigton_shape_t *) p;
    p += sizeof(bigton_shape_t) * header->numShapes;
    
    dest->functions = (bigton_function_t *) p;
    p += sizeof(bigton_function_t) * header->numFunctions;
    
    dest->builtinFunctions = (bigton_builtin_function_t *) p;
    p += sizeof(bigton_builtin_function_t) * header->numBuiltinFunctions;
    
    dest->props = (bigton_shape_prop_t *) p;
    p += sizeof(bigton_shape_prop_t) * header->numShapeProps;
    
    dest->constStringChars = (const bigton_char_t *) p;
    p += sizeof(bigton_char_t) * header->numConstStringChars;
    
    dest->instrTypes = (bigton_instr_type_t *) p;
    p += sizeof(bigton_instr_type_t) * header->numInstrs;
    
    const uint8_t *end = rawProgram + rawProgramSize;
    if (p > end) {
        r->error = BIGTONE_INT_INCOMPLETE_PROGRAM;
        return;
    }
}

static void allocateGlobals(bigton_runtime_state_t *r) {
    size_t globalsTSize = sizeof(bigton_value_type_t) * r->program.numGlobals;
    size_t globalsVSize = sizeof(bigton_value_t) * r->program.numGlobals;
    r->globalTypes = bigtonAllocNullableBuff(&r->b, globalsTSize);
    r->globalValues = bigtonAllocNullableBuff(&r->b, globalsVSize);
    for (size_t i = 0; i < r->program.numGlobals; i += 1) {
        r->globalTypes[i] = BIGTON_NULL;
    }
}

void bigtonInit(
    bigton_runtime_state_t *r,
    const bigton_runtime_settings_t *settings,
    const uint8_t *rawProgram, size_t rawProgramSize
) {
    r->error = BIGTONE_NONE;
    parseProgram(r, rawProgram, rawProgramSize);
    if (HAS_ERROR(r)) { return; }
    r->settings = *settings;
    r->b = (bigton_buff_owner_t) {
        .first = NULL,
        .last = NULL,
        .totalSizeBytes = 0
    };
    allocateGlobals(r);
    r->logsCapacity = 0;
    r->logsCount = 0;
    r->logs = NULL;
    r->traceCapacity = 0;
    r->traceCount = 0;
    r->trace = NULL;
    r->stack = BIGTON_VALUE_STACK_INIT;
    r->scopesCapacity = 0;
    r->scopesCount = 0;
    r->scopes = NULL;
    r->locals = BIGTON_VALUE_STACK_INIT;
    r->currentSource = (bigton_source_t) {
        .file = r->program.unknownStrId,
        .line = 0
    };
    r->currentInstr = r->program.globalStart;
    r->accCost = 0;
    r->awaitingBuiltinFun = 0;
    bigtonScopePush(r, (bigton_scope_t) {
        .type = BIGTONSC_GLOBAL,
        .start = r->program.globalStart,
        .end = r->program.globalEnd,
        .after = r->program.globalStart,
        .numLocals = 0
    });
}

void bigtonFree(bigton_runtime_state_t *r) {
    bigtonFreeAll(&r->b);
}