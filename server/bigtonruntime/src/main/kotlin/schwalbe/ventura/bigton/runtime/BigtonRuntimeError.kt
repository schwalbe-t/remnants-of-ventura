
package schwalbe.ventura.bigton.runtime

/**
 * Mirror of 'bigton_error_t'
 * defined in 'src/main/headers/bigton/runtime.h'
 */
enum class BigtonRuntimeError {
    NONE,

    INT_DIVISION_BY_ZERO,
    BY_PROGRAM,
    TUPLE_INDEX_OOB,
    INVALID_OBJECT_MEMBER,
    ARRAY_INDEX_OOB,
    OPERANDS_NOT_NUMBERS,
    OPERAND_NOT_INTEGER,
    OPERAND_NOT_TUPLE,
    OPERAND_NOT_OBJECT,
    OPERAND_NOT_ARRAY,
    EXCEEDED_MAXIMUM_CALL_DEPTH,
    TUPLE_TOO_BIG,
    EXCEEDED_MEMORY_LIMIT,
    
    INT_INCOMPLETE_PROGRAM,
    INT_INVALID_CONST_STRING,
    INT_STACK_EMPTY,
    INT_STACK_IDX_OOB,
    INT_SCOPE_STACK_EMPTY,
    INT_GLOBAL_VAR_REF_INVALID,
    INT_INSTR_COUNTER_OOB,
    INT_LOCAL_IDX_OOB,
    INT_SHAPE_PROP_IDX_OOB,
    INT_SHAPE_ID_OOB,
    INT_BUILTIN_FUN_REF_INVALID,
    INT_FUN_REF_INVALID;
    
    companion object {
        val allTypes = BigtonRuntimeError.values()
    }
}