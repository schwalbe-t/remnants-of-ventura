
package schwalbe.ventura.bigton

enum class BigtonErrorType(val id: String, val message: String) {

    // [TK___] - Failed Tokenization
    INVALID_TOKEN("TK001", "Program code uses an unrecognized token"),
    UNCLOSED_STRING_LITERAL("TK002", "Unclosed string literal"),
    MULTIPLE_DOTS_IN_NUMERIC("TK003", "Multiple dots in numeric literal"),

    // [PA___] - Failed Parsing
    CALLING_EXPRESSION("PA001", "Call on a non-function expression attempted"),
    INVALID_MEMBER_SYNTAX("PA002", "Accessed member must be (but isn't) either an integer (tuple) or identifier (object)"),
    MISSING_EXPECTED_COMMA("PA003", "Expected a comma, but got something else"),
    EMPTY_PARENTHESES("PA004", "Empty parentheses are not a valid expression"),
    MISSING_EXPECTED_MEMBER_NAME("PA005", "Expected the member name or closing brace in object literal, but got something else"),
    MISSING_EXPECTED_MEMBER_EQUALS("PA006", "Expected '=' after the member name in object literal, but got something else"),
    MISSING_EXPECTED_UNARY_OR_VALUE("PA007", "Expected a unary operator (e.g. '-', 'not') or a value (e.g. '5', '\"test\"'), but got something else"),
    MISSING_EXPECTED_OPENING_BRACE("PA008", "Expected '{' at the start of the instruction list, but got something else"),
    MISSING_EXPECTED_CLOSING_BRACE("PA009", "Expected '}' at the end of the instruction list, but got something else"),
    MISSING_EXPECTED_VARIABLE_NAME("PA010", "Expected the variable name after 'var', but got something else"),
    MISSING_EXPECTED_VAR_EQUALS("PA011", "Expected '=' after the variable name, but got something else"),
    MISSING_EXPECTED_FUNCTION_NAME("PA012", "Expected the function name after 'fun', but got something else"),
    MISSING_EXPECTED_FUNC_ARGS_OPEN("PA013", "Expected '(' after the function name, but got something else"),
    MISSING_EXPECTED_ARGUMENT_NAME("PA014", "Expected a function argument name in the function argument list, but got something else"),

    // [SC___] - Failed Static Checks
    FEATURE_UNSUPPORTED("SC001", "Feature not supported by the processor"),
    UNKNOWN_VARIABLE("SC002", "Program references a variable that is neither a global variable nor present in the current scope"),
    UNKNOWN_FUNCTION("SC003", "Program references a function that is neither defined by the program nor provided by an installed module"),
    ASSIGNMENT_TO_CONST("SC004", "Program attempted to assign a value to an expression that cannot be modified"),
    RETURN_OUTSIDE_FUNCTION("SC005", "'return' statement used outside of a function body"),
    LOOP_CONTROLS_OUTSIDE_LOOP("SC006", "'continue' or 'break' statement used outside of a loop body"),
    FUNCTION_INSIDE_FUNCTION("SC007", "Program defines a function inside of another function"),
    TOO_FEW_CALL_ARGS("SC008", "Program passes less arguments in a function call than the function requires"),
    TOO_MANY_CALL_ARGS("SC009", "Program passes more arguments in a function call than the function requires"),

    // [SC-INTERNAL___] - Internal Failed Static Checks
    INVALID_AST_ARG("SC-INTERNAL001", "AST node argument is invalid"),
    UNHANDLED_AST_TYPE("SC-INTERNAL002", "Code generation for used AST type not yet implemented"),

    // [RT___] - Runtime Error
    EXCEEDED_INSTR_LIMIT("RT001", "Ran too many instructions during this tick"),
    INT_DIVISION_BY_ZERO("RT002", "Attempted division of an integer by zero"),
    BY_PROGRAM("RT003", "Error indicated by program"),
    TUPLE_INDEX_OOB("RT004", "Tuple index too large for given tuple"),
    INVALID_OBJECT_MEMBER("RT005", "Object does not contain the given member"),
    ARRAY_INDEX_OOB("RT006", "Array index was out of bounds of the given array"),
    OPERANDS_NOT_NUMBERS("RT007", "The operand(s) of this operation should be (but arent't) either both integers or both floats"),
    OPERAND_NOT_INTEGER("RT008", "The operand of this operation should be (but isn't) an integer"),
    OPERAND_NOT_TUPLE("RT009", "The operand of this operation should be (but isn't) a tuple"),
    OPERAND_NOT_OBJECT("RT010", "The operand of this operation should be (but isn't) an object"),
    OPERAND_NOT_ARRAY("RT011", "The operand of this oepration should be (but isnt't) an array"),
    MAXIMUM_CALL_DEPTH("RT012", "Number of nested calls exceeded the maximum call depth allowed by this processor"),
    TUPLE_TOO_BIG("RT013", "Number of values contained by a created tuple exceeded the maximum allowed by the processor"),
    MAXIMUM_MEMORY_USAGE("RT014", "Out of memory"),
    
    // [RT-INTERNAL___] - Internal Runtime Error
    INCOMPLETE_PROGRAM("RT-INTERNAL001", "Runtime failed to load the program"),
    INVALID_CONST_STRING("RT-INTERNAL002", "A const string referenced by the program does not exist"),
    STACK_EMPTY("RT-INTERNAL003", "Attempt to pop from empty stack"),
    STACK_IDX_OOB("RT-INTERNAL004", "Attempt to access the stack at an invalid index"),
    SCOPE_STACK_EMPTY("RT-INTERNAL005", "Attempt to pop from empty scope stack"),
    GLOBAL_VAR_REF_INVALID("RT-INTERNAL006", "A global variable referenced by the program does not exist"),
    INSTR_COUNTER_OOB("RT-INTERNAL007", "Runtime attempted to load an instruction not present in the program"),
    LOCAL_IDX_OOB("RT-INTERNAL008", "A local variable referenced by the program does not exist"),
    SHAPE_PROP_IDX_OOB("RT-INTERNAL009", "An object shape property referenced by the program does not exist"),
    SHAPE_ID_OOB("RT-INTERNAL010", "An object shape referenced by the program does not exist"),
    BUILTIN_FUN_REF_INVALID("RT-INTERNAL011", "A builtin function referenced by the program does not exist"),
    FUN_REF_INVALID("RT-INTERNAL012", "A user-defined function referenced by the program does not exist"),
    
    // [RT-UNKOWN] - Unknown Runtime Error
    UNKNOWN("RT-UNKNOWN", "Runtime reported unknown error")

}

class BigtonException(val error: BigtonErrorType, val source: BigtonSource)
    : Exception(
        "[${error.id}] ${error.message} (line ${source.line}"
            + " in file '${source.file}')"
    )

data class BigtonSource(val line: Int, val file: String)

/**
 * Mirror of definition of 'bigton_error_t'
 * in 'bigtonruntime/include/bigton/error.h'
 */
enum class BigtonRuntimeError(val e: BigtonErrorType?) {
    NONE                        (null),
    
    EXCEEDED_INSTR_LIMIT        (BigtonErrorType.EXCEEDED_INSTR_LIMIT),
    INT_DIVISION_BY_ZERO        (BigtonErrorType.INT_DIVISION_BY_ZERO),
    BY_PROGRAM                  (BigtonErrorType.BY_PROGRAM),
    TUPLE_INDEX_OOB             (BigtonErrorType.TUPLE_INDEX_OOB),
    INVALID_OBJECT_MEMBER       (BigtonErrorType.INVALID_OBJECT_MEMBER),
    ARRAY_INDEX_OOB             (BigtonErrorType.ARRAY_INDEX_OOB),
    OPERANDS_NOT_NUMBERS        (BigtonErrorType.OPERANDS_NOT_NUMBERS),
    OPERAND_NOT_INTEGER         (BigtonErrorType.OPERAND_NOT_INTEGER),
    OPERAND_NOT_TUPLE           (BigtonErrorType.OPERAND_NOT_TUPLE),
    OPERAND_NOT_OBJECT          (BigtonErrorType.OPERAND_NOT_OBJECT),
    OPERAND_NOT_ARRAY           (BigtonErrorType.OPERAND_NOT_ARRAY),
    EXCEEDED_MAXIMUM_CALL_DEPTH (BigtonErrorType.MAXIMUM_CALL_DEPTH),
    TUPLE_TOO_BIG               (BigtonErrorType.TUPLE_TOO_BIG),
    EXCEEDED_MEMORY_LIMIT       (BigtonErrorType.MAXIMUM_MEMORY_USAGE),
    
    INT_INCOMPLETE_PROGRAM      (BigtonErrorType.INCOMPLETE_PROGRAM),
    INT_INVALID_CONST_STRING    (BigtonErrorType.INVALID_CONST_STRING),
    INT_STACK_EMPTY             (BigtonErrorType.STACK_EMPTY),
    INT_STACK_IDX_OOB           (BigtonErrorType.STACK_IDX_OOB),
    INT_SCOPE_STACK_EMPTY       (BigtonErrorType.SCOPE_STACK_EMPTY),
    INT_GLOBAL_VAR_REF_INVALID  (BigtonErrorType.GLOBAL_VAR_REF_INVALID),
    INT_INSTR_COUNTER_OOB       (BigtonErrorType.INSTR_COUNTER_OOB),
    INT_LOCAL_IDX_OOB           (BigtonErrorType.LOCAL_IDX_OOB),
    INT_SHAPE_PROP_IDX_OOB      (BigtonErrorType.SHAPE_PROP_IDX_OOB),
    INT_SHAPE_ID_OOB            (BigtonErrorType.SHAPE_ID_OOB),
    INT_BUILTIN_FUN_REF_INVALID (BigtonErrorType.BUILTIN_FUN_REF_INVALID),
    INT_FUN_REF_INVALID         (BigtonErrorType.FUN_REF_INVALID);
}

val bigtonRuntimeErrors: Array<BigtonRuntimeError>
    = BigtonRuntimeError.values()
    
fun Int.toBigtonError(): BigtonErrorType
    = bigtonRuntimeErrors.getOrNull(this)?.e
    ?: BigtonErrorType.UNKNOWN