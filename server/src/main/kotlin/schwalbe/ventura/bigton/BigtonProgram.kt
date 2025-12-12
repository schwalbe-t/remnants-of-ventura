
package schwalbe.ventura.bigton

data class BigtonProgram(
    val functions: Map<String, List<BigtonInstr>>,
    val global: List<BigtonInstr>
)

// BIGTON IR CALLING CONVENTIONS
//
// 1. Call arguments are pushed onto stack in normal order
// 2. 'CALL' executed with function name
// 3. Call arguments are poped from stack in reverse order
//    ... (function body)
// 4. If explicit return:
//      5. Return value is pushed onto stack
//      6. 'RETURN' is executed
// 4. Else:
//      5. (implicit) null is pushed onto stack 
// 5. Return value is on stack at call site

enum class BigtonInstrType {

    // arg: Int = source line
    // stack: ->
    SOURCE_LINE,

    // arg: BigtonValue = value
    // stack: -> <arg>
    LOAD_VALUE,
    // arg: Int = num elements
    // stack: a, b, c, ... -> <tuple>
    LOAD_TUPLE,
    // arg: Int = tuple index
    // stack: tuple -> <member_value>
    LOAD_TUPLE_MEMBER,
    // arg: List<String> = member names [a, b, c, ...]
    // stack: a, b, c, ... -> <object>
    LOAD_OBJECT,
    // arg: String = member name
    // stack: object -> <member_value>
    LOAD_OBJECT_MEMBER,
    // arg: String = variable name
    // stack: -> <var_value>
    LOAD_VARIABLE,
    // arg: null
    // stack: address -> <value_at_address>
    LOAD_MEMORY,

    // arg: null
    // stack: a, b -> (a + b)
    ADD,
    // arg: null
    // stack: a, b -> (a - b)
    SUBTRACT,
    // arg: null
    // stack: a, b -> (a * b)
    MULTIPLY,
    // arg: null
    // stack: a, b -> (a / b)
    DIVIDE,
    // arg: null
    // stack: a, b -> (a % b)
    REMAINDER,
    // arg: null,
    // stack: x -> (-x)
    NEGATE,

    // arg: null
    // stack: a, b -> (a < b)
    LESS_THAN,
    // arg: null
    // stack: a, b -> (a <= b)
    LESS_THAN_EQUAL,
    // arg: null
    // stack: a, b -> (a > b)
    GREATER_THAN,
    // arg: null
    // stack: a, b -> (a >= b)
    GREATER_THAN_EQUAL,
    // arg: null
    // stack: a, b -> (a == b)
    EQUAL,
    // arg: null
    // stack: a, b -> (a != b)
    NOT_EQUAL,

    // NOTE: NOT LAZY
    // arg: null
    // stack: a, b -> (a && b)
    AND,
    // NOTE: NOT LAZY
    // arg: null
    // stack: a, b -> (a || b)
    OR,
    // arg: null
    // stack: x -> (!x)
    NOT,


    // arg: String = variable name
    // stack: value ->
    STORE_EXISTING_VARIABLE,
    // arg: String = variable name
    // stack: value ->
    STORE_NEW_VARIABLE,
    // arg: null
    // stack: address, value ->
    STORE_MEMORY,
    // arg: String = member name
    // stack: object, value ->
    STORE_OBJECT_MEMBER,

    // arg: Pair<List<BigtonInstr>, List<BigtonInstr>?> = (if_body, else_body)
    // stack: condition ->
    IF,
    // arg: List<BigtonInstr> = body
    // stack: ->
    LOOP,
    // arg: List<BigtonInstr> = body
    // stack: ->
    TICK,
    // arg: null
    // stack: ->
    CONTINUE,
    // arg: null
    // stack: ->
    BREAK,
    // arg: String = name
    // stack: a, b, c, ... -> <return_value>
    CALL,
    // arg: null
    // stack: return_value -> <return_value>
    RETURN

}

data class BigtonInstr(
    val type: BigtonInstrType,
    val arg: Any? = null
)

inline fun<reified T> BigtonInstr.castArg(currentLine: Int): T
    = this.arg as? T
    ?: throw BigtonException(BigtonErrorType.INVALID_INSTR_ARG, currentLine)