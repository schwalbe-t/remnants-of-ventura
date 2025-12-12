
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

    // [SC-INTERNAL___] - Internal Failed Static Checks
    INVALID_AST_ARG("SC-INTERNAL001", "AST node argument is invalid"),

    // [RT___] - Runtime Error
    EXCEEDED_INSTR_LIMIT("RT001", "Ran too many instructions during this tick"),
    INT_DIVISION_BY_ZERO("RT002", "Attempted division of an integer by zero"),
    MEMORY_ADDRESS_NEGATIVE("RT003", "Accessed memory address is negative"),
    MEMORY_ADDRESS_TOO_LARGE("RT004", "Accessed memory address it too large"),
    BY_PROGRAM("RT005", "Error indicated by program"),
    TUPLE_INDEX_OOB("RT006", "Tuple index too large"),
    INVALID_OBJECT_MEMBER("RT007", "Object does not contain the given member"),
    OPERANDS_NOT_NUMBERS("RT008", "The operand(s) of this operation should be (but arent't) either both integers or both floats"),
    OPERAND_NOT_INTEGER("RT009", "The operand of this operation should be (but isn't) an integer"),
    OPERAND_NOT_TUPLE("RT010", "The operand of this operation should be (but isn't) a tuple"),
    OPERAND_NOT_OBJECT("RT011", "The operand of this operation should be (but isn't) an object"),

    // [RT-INTERNAL___] - Internal Runtime Error
    MISSING_OPERAND("RT-INTERNAL001", "Missing operand from operand stack"),
    INVALID_INSTR_ARG("RT-INTERNAL002", "Instruction argument is invalid"),
    MISSING_VARIABLE("RT-INTERNAL003", "Variable does not exist at runtime"),
    MISSING_FUNCTION("RT-INTERNAL004", "Function does not exist at runtime")

}

class BigtonException(val error: BigtonErrorType, val line: Int)
    : Exception("[${error.id}] ${error.message} (line ${line + 1})")