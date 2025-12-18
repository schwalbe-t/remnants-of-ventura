
package schwalbe.ventura.bigton.runtime

import schwalbe.ventura.bigton.*

data class BigtonProgram(
    val functions: Map<String, List<BigtonInstr>>,
    val global: List<BigtonInstr>
)

fun BigtonProgram.displayInstr(): String {
    val functions: String = this.functions
        .map { (name, body) -> "[$name] ${body.displayInstr()}" }
        .joinToString("\n\n")
    val global: String = this.global
        .map { i -> i.displayInstr() }
        .joinToString("\n")
    return "$functions\n\n$global"
}

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

enum class BigtonInstrType {

    // arg: Int = source line
    // stack: ->
    SOURCE_LINE,
    // arg: null
    // stack: value ->
    DISCARD,

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
    LOAD_GLOBAL,
    // arg: Int = relative local index
    // stack: -> <var_value>
    LOAD_LOCAL,
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
    STORE_GLOBAL,
    // arg: null
    // stack: value ->
    PUSH_LOCAL,
    // arg: Int = relative local index
    // stack: value ->
    STORE_LOCAL,
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

fun BigtonInstr.displayInstr(): String = when (this.type) {
    BigtonInstrType.SOURCE_LINE
        -> "SOURCE_LINE ${this.castArg<Int>(-1)}"
    BigtonInstrType.DISCARD -> "DISCARD"
    BigtonInstrType.LOAD_VALUE -> {
        val arg: Any? = this.arg
        "LOAD_VALUE " + when (arg) {
            is BigtonNull -> "null"
            is BigtonInt -> arg.v
            is BigtonFloat -> arg.v
            is BigtonString -> "\"${arg.v}\""
            else -> arg.toString()
        }
    }
    BigtonInstrType.LOAD_TUPLE
        -> "LOAD_TUPLE ${this.castArg<Int>(-1)}"
    BigtonInstrType.LOAD_TUPLE_MEMBER
        -> "LOAD_TUPLE_MEMBER ${this.castArg<Int>(-1)}"
    BigtonInstrType.LOAD_OBJECT
        -> "LOAD_OBJECT " + this.castArg<List<String>>(-1)
            .map { m -> "\"$m\"" }.joinToString(", ")
    BigtonInstrType.LOAD_OBJECT_MEMBER
        -> "LOAD_OBJECT_MEMBER \"${this.castArg<String>(-1)}\""
    BigtonInstrType.LOAD_GLOBAL
        -> "LOAD_GLOBAL \"${this.castArg<String>(-1)}\""
    BigtonInstrType.LOAD_LOCAL
        -> "LOAD_LOCAL ${this.castArg<Int>(-1)}"
    BigtonInstrType.LOAD_MEMORY -> "LOAD_MEMORY"
    BigtonInstrType.ADD -> "ADD"
    BigtonInstrType.SUBTRACT -> "SUBTRACT"
    BigtonInstrType.MULTIPLY -> "MULTIPLY"
    BigtonInstrType.DIVIDE -> "DIVIDE"
    BigtonInstrType.REMAINDER -> "REMAINDER"
    BigtonInstrType.NEGATE -> "NEGATE"
    BigtonInstrType.LESS_THAN -> "LESS_THAN"
    BigtonInstrType.LESS_THAN_EQUAL -> "LESS_THAN_EQUAL"
    BigtonInstrType.GREATER_THAN -> "GREATER_THAN"
    BigtonInstrType.GREATER_THAN_EQUAL -> "GREATER_THAN_EQUAL"
    BigtonInstrType.EQUAL -> "EQUAL"
    BigtonInstrType.NOT_EQUAL -> "NOT_EQUAL"
    BigtonInstrType.AND -> "AND"
    BigtonInstrType.OR -> "OR"
    BigtonInstrType.NOT -> "NOT"
    BigtonInstrType.STORE_GLOBAL
        -> "STORE_GLOBAL \"${this.castArg<String>(-1)}\""
    BigtonInstrType.PUSH_LOCAL -> "PUSH_LOCAL"
    BigtonInstrType.STORE_LOCAL
        -> "STORE_LOCAL ${this.castArg<Int>(-1)}"
    BigtonInstrType.STORE_MEMORY -> "STORE_MEMORY"
    BigtonInstrType.STORE_OBJECT_MEMBER
        -> "STORE_OBJECT_MEMBER \"${this.castArg<String>(-1)}\""
    BigtonInstrType.IF -> {
        val (if_body, else_body)
            = this.castArg<Pair<List<BigtonInstr>, List<BigtonInstr>?>>(-1)
        "IF ${if_body.displayInstr()} ${else_body?.displayInstr() ?: ""}"
    }
    BigtonInstrType.LOOP
        -> "LOOP ${this.castArg<List<BigtonInstr>>(-1).displayInstr()}"
    BigtonInstrType.TICK
        -> "TICK ${this.castArg<List<BigtonInstr>>(-1).displayInstr()}"
    BigtonInstrType.CONTINUE -> "CONTINUE"
    BigtonInstrType.BREAK -> "BREAK"
    BigtonInstrType.CALL
        -> "CALL \"${this.castArg<String>(-1)}\""
    BigtonInstrType.RETURN -> "RETURN"
}

fun List<BigtonInstr>.displayInstr(): String {
    val raw: String = this
        .map { i -> i.displayInstr() }
        .joinToString("\n")
    val indented: String = raw
        .split("\n")
        .map { l -> "    $l" }
        .joinToString("\n")
    return "{\n$indented\n}"
}