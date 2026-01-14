
#ifndef BIGTON_IR_H
#define BIGTON_IR_H

#include <stdint.h>

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

enum BigtonInstrType {
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
    // arg:
    // stack: value ->
    BIGTONIR_PUSH_LOCAL,
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
    // arg: bigton_instr_idx_t infLoopLength
    // stack: ->
    BIGTONIR_LOOP,
    // arg: bigton_instr_idx_t tickLoopLength
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
    // arg: bigton_slot_t calledBuiltin
    // stack: a, b, c, ... -> <return_value>
    BIGTONIR_CALL_BUILTIN,
    // arg:
    // stack: return_value -> <return_value>
    BIGTONIR_RETURN
};
typedef uint8_t bigton_instr_type_t; // enum BigtonInstrType


typedef uint32_t bigton_str_id_t;
typedef uint16_t bigton_char_t;
typedef int64_t bigton_int_t;
typedef double bigton_float_t;
typedef uint32_t bigton_slot_t;
typedef uint32_t bigton_instr_idx_t;
typedef uint32_t bigton_shape_id_t;

typedef struct BigtonShapeProp {
    bigton_str_id_t name;
    uint32_t offset;
} bigton_shape_prop_t;
typedef struct BigtonShape {
    uint32_t propCount;
    uint32_t firstPropOffset;
} bigton_shape_t;

typedef struct BigtonIfArgs {
    bigton_instr_idx_t if_body_length;
    bigton_instr_idx_t else_body_length;
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
    bigton_instr_idx_t infLoopLength;
    bigton_instr_idx_t tickLoopLength;
    bigton_slot_t called;
    bigton_slot_t calledBuiltin;
} bigton_instr_args_t;

typedef struct BigtonSource {
    bigton_str_id_t file;
    uint32_t line;
} bigton_source_t;

typedef struct BigtonFunction {
    bigton_str_id_t name;
    bigton_source_t declSource;
    bigton_instr_idx_t start;
    bigton_instr_idx_t length;
} bigton_function_t;

typedef struct BigtonBuiltinFunction {
    bigton_str_id_t name;
    uint32_t cost;
} bigton_builtin_function_t;

typedef struct BigtonConstString {
    uint64_t firstOffset;
    uint64_t charLength;
} bigton_const_string_t;

typedef struct BigtonProgram {
    bigton_instr_idx_t numInstrs;
    bigton_str_id_t numStrings;
    bigton_shape_id_t numShapes;
    
    bigton_slot_t numFunctions;
    bigton_slot_t numBuiltinFunctions;
    bigton_slot_t numGlobalVars;
    uint32_t numShapeProps;
    
    uint64_t numConstStringChars; 
    
    bigton_str_id_t unknownStrId;
    bigton_instr_idx_t globalStart;
    bigton_instr_idx_t globalLength;
} bigton_program_t;

// FILE FORMAT STRUCTURE:
// 
// bigton_program_t header;
// 
// --- alignment = 8 ---
// bigton_instr_args_t instrArgs[header.numInstrs];
// bigton_const_string_t constStrings[header.numStrings];
// bigton_shape_t shapes[header.numShapes];
// 
// --- alignment = 4 ---
// bigton_function_t functions[header.numFunctions];
// bigton_builtin_function_t builtinFunctions[header.numBuiltinFunctions];
// bigton_shape_prop_t shapeProps[header.numShapeProps];
// 
// --- alignment = 2 ---
// bigton_char_t constStringChars[header.numConstStringChars];
// 
// --- alignment = 1 ---
// bigton_instr_type_t instrTypes[header.numInstrs];

#endif