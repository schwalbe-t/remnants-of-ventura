
#ifndef BIGTON_RUNTIME_H
#define BIGTON_RUNTIME_H

#include "bigton.h"
#include "bigton_ir.h"

typedef enum BigtonError {
    BIGTONE_NONE,
    
    BIGTONE_EXCEEDED_INSTR_LIMIT,
    BIGTONE_INT_DIVISION_BY_ZERO,
    BIGTONE_BY_PROGRAM,
    BIGTONE_TUPLE_INDEX_OOB,
    BIGTONE_INVALID_OBJECT_MEMBER,
    BIGTONE_ARRAY_INDEX_OOB,
    BIGTONE_OPERANDS_NOT_NUMBERS,
    BIGTONE_OPERAND_NOT_INTEGER,
    BIGTONE_OPERAND_NOT_TUPLE,
    BIGTONE_OPERAND_NOT_OBJECT,
    BIGTONE_OPERAND_NOT_ARRAY,
    BIGTONE_EXCEEDED_MAXIMUM_CALL_DEPTH,
    BIGTONE_TUPLE_TOO_BIG,
    BIGTONE_EXCEEDED_MEMORY_LIMIT,
    
    BIGTONE_INT_INCOMPLETE_PROGRAM,
    BIGTONE_INT_INVALID_CONST_STRING,
    BIGTONE_INT_STACK_EMPTY,
    BIGTONE_INT_STACK_IDX_OOB,
    BIGTONE_INT_SCOPE_STACK_EMPTY,
    BIGTONE_INT_GLOBAL_VAR_REF_INVALID,
    BIGTONE_INT_INSTR_COUNTER_OOB,
    BIGTONE_INT_LOCAL_IDX_OOB
} bigton_error_t;

typedef struct BigtonParsedProgram {
    bigton_str_id_t unknownStrId;
    
    bigton_instr_idx_t numInstrs;
    bigton_instr_type_t *instrTypes;
    bigton_instr_args_t *instrArgs;
    
    bigton_str_id_t numConstStrings;
    bigton_const_string_t *constStrings;
    size_t numConstStringChars;
    const bigton_char_t *constStringChars;
    
    bigton_shape_id_t numShapes;
    bigton_shape_t *shapes;
    bigton_shape_prop_t *props;
    
    bigton_slot_t numFunctions;
    bigton_function_t *functions;
    
    bigton_slot_t numGlobals;
    bigton_instr_idx_t globalStart;
    bigton_instr_idx_t globalEnd;
} bigton_parsed_program_t;

typedef struct BigtonRuntimeSettings {
    uint64_t tickInstructionLimit;
    uint32_t maxCallDepth;
    uint32_t maxTupleSize;
} bigton_runtime_settings_t;

typedef enum BigtonScopeType {
    BIGTONSC_GLOBAL,
    BIGTONSC_FUNCTION,
    BIGTONSC_LOOP,
    BIGTONSC_TICK,
    BIGTONSC_IF
} bigton_scope_type_t;

typedef struct BigtonScope {
    bigton_scope_type_t type;
    bigton_instr_idx_t start;
    bigton_instr_idx_t end;
    bigton_instr_idx_t after;
    bigton_slot_t numLocals;
} bigton_scope_t;

typedef struct BigtonValueStack {
    size_t capacity;
    size_t count;
    bigton_value_type_t *types;
    bigton_value_t *values;
} bigton_value_stack_t;

#define BIGTON_VALUE_STACK_INIT ((bigton_value_stack_t) { \
    .capacity = 0, .count = 0, .types = NULL, .values = NULL \
})

typedef struct BigtonRuntimeState {
    bigton_parsed_program_t program;
    bigton_runtime_settings_t settings;

    bigton_value_type_t *globalTypes;
    bigton_value_t *globalValues;
    
    size_t logsCapacity;
    size_t logsCount;
    bigton_string_t **logs;
    
    bigton_value_stack_t stack;
    
    size_t scopesCapacity;
    size_t scopesCount;
    bigton_scope_t *scopes;
    
    bigton_value_stack_t locals;
    
    bigton_error_t error;
    bigton_source_t currentSource;
    bigton_instr_idx_t currentInstr;
} bigton_runtime_state_t;


void bigtonInit(
    bigton_runtime_state_t *r,
    const bigton_runtime_settings_t *settings,
    const uint8_t *rawProgram, size_t rawProgramSize
);
void bigtonFree(bigton_runtime_state_t *r);


typedef enum BigtonExecStatus {
    BIGTONST_CONTINUE,
    BIGTONST_AWAIT_TICK,
    BIGTONST_COMPLETE,
    BIGTONST_ERROR
} bigton_exec_status_t;

void bigtonLogLine(bigton_runtime_state_t *r, bigton_string_t *line);

void bigtonStackPush(bigton_value_stack_t *s, bigton_tagged_value_t value);
bigton_tagged_value_t bigtonStackSet(
    bigton_value_stack_t *s, size_t i, bigton_tagged_value_t v,
    bigton_error_t *e
);
bigton_tagged_value_t bigtonStackAt(
    bigton_value_stack_t *s, size_t i, bigton_error_t *e
);
bigton_tagged_value_t bigtonStackPop(
    bigton_value_stack_t *s, bigton_error_t *e
);

void bigtonScopePush(bigton_runtime_state_t *r, bigton_scope_t scope);
bigton_scope_t *bigtonScopeCurr(bigton_runtime_state_t *r);
void bigtonScopePop(bigton_runtime_state_t *r);

bigton_exec_status_t bigtonExecInstr(bigton_runtime_state_t *r);
bigton_exec_status_t bigtonExecTick(bigton_runtime_state_t *r);


bigton_string_t *bigtonAllocConstString(
    bigton_runtime_state_t *r, bigton_str_id_t id
);
bigton_string_t *bigtonAllocString(
    uint64_t length, const bigton_char_t *content
);

#ifdef BIGTON_ERROR_MACROS
    #define HAS_ERROR(r) ((r)->error != BIGTONE_NONE)
#endif

#endif