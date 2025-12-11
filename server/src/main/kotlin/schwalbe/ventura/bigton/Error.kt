
package schwalbe.ventura.bigton

enum class BigtonErrorType(val id: String, val message: String) {

    // [TK___] - Failed Tokenization
    INVALID_TOKEN("TK001", "Program code uses unrecognized token"),

    // [PA___] - Failed Parsing
    UNEXPECTED_EOF("PA001", "Program code unexpectedly ended"),
    UNEXPECTED_TOKEN("PA002", "Program code uses token at unexpected location"),

    // [SC___] - Failed Static Checks
    FEATURE_UNSUPPORTED("SC001", "Feature not supported by the processor"),

    // [RT___] - Runtime Error
    EXCEEDED_INSTR_LIMIT("RT001", "Ran too many instructions during this tick"),
    INT_DIVISION_BY_ZERO("RT002", "Attempted division of an integer by zero"),
    MEMORY_ADDRESS_NEGATIVE("RT003", "Accessed memory address is negative"),
    MEMORY_ADDRESS_TOO_LARGE("RT004", "Accessed memory address it too large"),
    INVALID_TYPE("RT005", "Values of the given type unsupported by operation"),
    BY_PROGRAM("RT006", "Error indicated by program"),
    TUPLE_INDEX_OOB("RT007", "Tuple index too large"),

    // [INTERNAL___] - Internal Runtime Error
    MISSING_OPERAND("INTERNAL001", "Missing operand from operand stack"),
    INVALID_INSTR_ARG("INTERNAL002", "Instruction argument is invalid"),
    MISSING_VARIABLE("INTERNAL003", "Variable does not exist at runtime"),
    MISSING_FUNCTION("INTERNAL004", "Function does not exist at runtime")

}

class BigtonException(val error: BigtonErrorType, val line: Int)
    : Exception("[${error.id}] ${error.message} (line ${line + 1})")