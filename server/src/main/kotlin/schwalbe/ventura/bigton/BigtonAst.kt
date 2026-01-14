
package schwalbe.ventura.bigton

data class BigtonAstFunction(
    val name: String,
    val argNames: List<String>,
    val body: List<BigtonAst>
)

enum class BigtonAstType {

    // simple "value" expressions
    IDENTIFIER,     //                      name: String
    NULL_LITERAL,
    INT_LITERAL,    //                      value: String
    FLOAT_LITERAL,  //                      value: String
    STRING_LITERAL, //                      value: String
    TUPLE_LITERAL,  // [...element_vals]
    OBJECT_LITERAL, // [...member_vals]     members: List<String>
    CALL,           // [...arg_vals]        function: String
    
    // unary operators
    DEREF,          // [address]
    NOT,            // [x]
    NEGATE,         // [x]

    // binary operators (fixed second operand)
    TUPLE_MEMBER,   // [tuple]      index: Int
    OBJECT_MEMBER,  // [object]     member: String

    // binary operators (dynamic second operand) 
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    REMAINDER,
    LESS_THAN,
    GREATER_THAN,
    LESS_THAN_EQUAL,
    GREATER_THAN_EQUAL,
    EQUAL,
    NOT_EQUAL,
    AND,
    OR,

    // statements
    ASSIGNMENT,     // [dest, val]
    IF,             // [cond]       Pair<List<BigtonAst>, List<BigtonAst>?>
    LOOP,           //              List<BigtonAst>
    TICK,           //              List<BigtonAst>
    WHILE,          // [cond]       List<BigtonAst>
    CONTINUE,
    BREAK,
    RETURN,         // [value]
    VARIABLE,       // [value]      name: String

    // function decl
    FUNCTION        //              BigtonAstFunction
    
}

data class BigtonAst(
    val type: BigtonAstType,
    val source: BigtonSource,
    val arg: Any? = null,
    val children: List<BigtonAst> = emptyList()
)

inline fun<reified T> BigtonAst.castArg(): T
    = this.arg as? T
    ?: throw BigtonException(BigtonErrorType.INVALID_AST_ARG, this.source)