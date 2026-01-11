
#ifndef BIGTON_IR_H
#define BIGTON_IR_H

#include "bigton_types.h"

// BIGTON IR CALLING CONVENTIONS
//
// 1. Call arguments are pushed onto stack in normal order
// 2. 'CALL' executed with function name
// 3. Call arguments are popped from stack in reverse order
//    ... (function body)
// 4. If explicit return:
//      5. Return value is pushed onto stack
//      6. 'RETURN' is executed
// 4. Else:
//      5. (implicit) null is pushed onto stack 
// 5. Return value is on stack at call site

typedef enum BigtonInstrType {
    // arg: uint32_t sourceLine
    // stack: ->
    BIGTONIR_SOURCE_LINE,
    // arg: bigton_str_id_t sourceFile
    // stack: ->
    BIGTONIR_SOURCE_FILE,
    // arg:
    // stack: value ->
    BIGTONIR_DISCARD,
    
    // arg:
    // stack: -> null
    BIGTONIR_LOAD_NULL,
    // arg: bigton_int_t loadInt
    // stack: -> <loadInt>
    BIGTONIR_LOAD_INT,
    // arg: bigton_float_t loadFloat
    // stack: -> <loadFloat>
    BIGTONIR_LOAD_FLOAT,
    // arg: bigton_str_id_t loadString
    // stack: -> <loadString>
    BIGTONIR_LOAD_STRING,
    // arg: uint32_t loadTupleLength
    // stack: a, b, c, ... -> <tuple>
    BIGTONIR_LOAD_TUPLE,
    // arg: bigton_shape_id_t loadObject
    // stack: a, b, c, ... -> <object>
    BIGTONIR_LOAD_OBJECT,
    // arg: uint32_t loadArrayLength
    // stack: a, b, c, ... -> <array>
    BIGTONIR_LOAD_ARRAY,
    
    // arg: uint32_t loadTupleMemIdx
    // stack: tuple -> <member_value>
    BIGTONIR_LOAD_TUPLE_MEMBER,
    // arg: bigton_str_id_t loadObjectMemName
    // stack: object -> <member_value>
    BIGTONIR_LOAD_OBJECT_MEMBER,
    // arg:
    // stack: array, index -> <element_value>
    BIGTONIR_LOAD_ARRAY_ELEMENT,
    // arg: bigton_slot_t loadGlobal
    // stack: -> <var_value>
    BIGTONIR_LOAD_GLOBAL,
    // arg: bigton_slot_t loadLocal
    // stack: -> <var_value>
    BIGTONIR_LOAD_LOCAL,
    
    // arg:
    // stack: a, b -> (a + b)
    BIGTONIR_ADD,
    // arg:
    // stack: a, b -> (a - b)
    BIGTONIR_SUBTRACT,
    // arg:
    // stack: a, b -> (a * b)
    BIGTONIR_MULTIPLY,
    // arg:
    // stack: a, b -> (a / b)
    BIGTONIR_DIVIDE,
    // arg:
    // stack: a, b -> (a % b)
    BIGTONIR_REMAINDER,
    // arg:,
    // stack: x -> (-x)
    BIGTONIR_NEGATE,
    
    // arg:
    // stack: a, b -> (a < b)
    BIGTONIR_LESS_THAN,
    // arg:
    // stack: a, b -> (a <= b)
    BIGTONIR_LESS_THAN_EQUAL,
    // arg:
    // stack: a, b -> (a > b)
    BIGTONIR_GREATER_THAN,
    // arg:
    // stack: a, b -> (a >= b)
    BIGTONIR_GREATER_THAN_EQUAL,
    // arg:
    // stack: a, b -> (a == b)
    BIGTONIR_EQUAL,
    // arg:
    // stack: a, b -> (a != b)
    BIGTONIR_NOT_EQUAL,
    
    // NOTE: NOT LAZY
    // arg:
    // stack: a, b -> (a && b)
    BIGTONIR_AND,
    // NOTE: NOT LAZY
    // arg:
    // stack: a, b -> (a || b)
    BIGTONIR_OR,
    // arg:
    // stack: x -> (!x)
    BIGTONIR_NOT,
    
    // arg: bigton_slot_t storeGlobal
    // stack: value ->
    BIGTONIR_STORE_GLOBAL,
    // arg: bigton_slot_t storeLocal
    // stack: value ->
    BIGTONIR_STORE_LOCAL,
    // arg: bigton_str_id_t storeObjectMemName
    // stack: object, value ->
    BIGTONIR_STORE_OBJECT_MEMBER,
    // arg:
    // stack: array, index, value ->
    BIGTONIR_STORE_ARRAY_ELEMENT,
    
    // arg: bigton_if_args_t ifParams
    // stack: condition ->
    BIGTONIR_IF,
    // arg: uint32_t infLoopLength
    // stack: ->
    BIGTONIR_LOOP,
    // arg: uint32_t tickLoopLength
    // stack: ->
    BIGTONIR_TICK,
    // arg:
    // stack: ->
    BIGTONIR_CONTINUE,
    // arg:
    // stack: ->
    BIGTONIR_BREAK,
    // arg: bigton_slot_t called
    // stack: a, b, c, ... -> <return_value>
    BIGTONIR_CALL,
    // arg:
    // stack: return_value -> <return_value>
    BIGTONIR_RETURN
} bigton_instr_type_t;

typedef uint32_t bigton_slot_t;
typedef uint32_t bigton_shape_id_t;

typedef struct BigtonIfArgs {
    uint32_t if_body_length;
    uint32_t else_body_length;
} bigton_if_args_t;

typedef union BigtonInstrArg {
    uint32_t sourceLine;
    bigton_str_id_t sourceFile;
    
    bigton_int_t loadInt;
    bigton_float_t loadFloat;
    bigton_str_id_t loadString;
    uint32_t loadTupleLength;
    bigton_shape_id_t loadObject;
    uint32_t loadArrayLength;
    
    uint32_t loadTupleMemIdx;
    bigton_str_id_t loadObjectMemName;
    bigton_slot_t loadGlobal;
    bigton_slot_t loadLocal;
    
    bigton_slot_t storeGlobal;
    bigton_slot_t storeLocal;
    bigton_str_id_t storeObjectMemName;
    
    bigton_if_args_t ifParams;
    uint32_t infLoopLength;
    uint32_t tickLoopLength;
    bigton_slot_t called;
} bigton_instr_params_t;

typedef enum BigtonSymbolType {
    BIGTONIR_FUNCTION,
    BIGTONIR_VARIABLE
} bigton_symbol_type_t;

#endif