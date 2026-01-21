
#ifndef BIGTON_RUNTIME_H
#define BIGTON_RUNTIME_H

#include <bigton/values.h>
#include <bigton/ir.h>
#include <bigton/error.h>

typedef struct BigtonParsedProgram {
    const uint8_t *rawProgramBuffer;

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
    size_t numProps;
    bigton_shape_prop_t *props;
    
    bigton_slot_t numFunctions;
    bigton_function_t *functions;
    bigton_slot_t numBuiltinFunctions;
    bigton_builtin_function_t *builtinFunctions;
    
    bigton_slot_t numGlobals;
    bigton_instr_idx_t globalStart;
    bigton_instr_idx_t globalEnd;
} bigton_parsed_program_t;

typedef struct BigtonRuntimeSettings {
    uint64_t tickInstructionLimit;
    size_t memoryUsageLimit;
    uint32_t maxCallDepth;
    uint32_t maxTupleSize;
} bigton_runtime_settings_t;

typedef struct BigtonTraceCall {
    bigton_str_id_t name;
    bigton_source_t calledFrom;
    bigton_source_t definedAt;
} bigton_trace_call_t;

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

    bigton_buff_owner_t b;
    
    bigton_value_type_t *globalTypes;
    bigton_value_t *globalValues;
    
    size_t logsCapacity;
    size_t logsCount;
    bigton_string_t **logs;
    
    size_t traceCapacity;
    size_t traceCount;
    bigton_trace_call_t *trace;
    
    bigton_value_stack_t stack;
    
    size_t scopesCapacity;
    size_t scopesCount;
    bigton_scope_t *scopes;
    
    bigton_value_stack_t locals;
    
    bigton_error_t error;
    bigton_source_t currentSource;
    bigton_instr_idx_t currentInstr;
    size_t accCost;
    bigton_slot_t awaitingBuiltinFun;
} bigton_runtime_state_t;


void bigtonParseProgram(
    const uint8_t *rawProgram, size_t rawProgramSize,
    bigton_parsed_program_t *p, bigton_error_t *e
);
void bigtonInit(
    bigton_runtime_state_t *r,
    const bigton_runtime_settings_t *settings,
    const bigton_parsed_program_t* p
);
void bigtonFree(bigton_runtime_state_t *r);


void bigtonDebugProgram(bigton_parsed_program_t *p);

typedef enum BigtonExecStatus {
    BIGTONST_CONTINUE,
    BIGTONST_EXEC_BUILTIN_FUN,
    BIGTONST_AWAIT_TICK,
    BIGTONST_COMPLETE,
    BIGTONST_ERROR
} bigton_exec_status_t;

void bigtonLogLine(bigton_runtime_state_t *r, bigton_string_t *line);

void bigtonTracePush(bigton_runtime_state_t *r, bigton_trace_call_t t);
void bigtonTracePop(bigton_runtime_state_t *r);

void bigtonStackPush(
    bigton_value_stack_t *s, bigton_tagged_value_t value,
    bigton_runtime_state_t *r
);
bigton_tagged_value_t bigtonStackSet(
    bigton_value_stack_t *s, size_t i, bigton_tagged_value_t v,
    bigton_runtime_state_t *r
);
bigton_tagged_value_t bigtonStackAt(
    bigton_value_stack_t *s, size_t i, bigton_runtime_state_t *r
);
bigton_tagged_value_t bigtonStackPop(
    bigton_value_stack_t *s, bigton_runtime_state_t *r
);

void bigtonScopePush(bigton_runtime_state_t *r, bigton_scope_t scope);
bigton_scope_t *bigtonScopeCurr(bigton_runtime_state_t *r);
void bigtonScopePop(bigton_runtime_state_t *r);

void bigtonStartTick(bigton_runtime_state_t *r);
bigton_exec_status_t bigtonExecInstr(bigton_runtime_state_t *r);
bigton_exec_status_t bigtonExecBatch(bigton_runtime_state_t *r);


bigton_string_t *bigtonAllocConstString(
    bigton_runtime_state_t *r, bigton_str_id_t id
);
bigton_string_t *bigtonAllocString(
    bigton_buff_owner_t *o, uint64_t length, const bigton_char_t *content
);

#ifdef BIGTON_ERROR_MACROS
    #define HAS_ERROR(r) ((r)->error != BIGTONE_NONE)
#endif

#endif