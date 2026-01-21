
package schwalbe.ventura.bigton

enum class BigtonTokenType {

    IDENTIFIER,
    INT_LITERAL,
    FLOAT_LITERAL,
    STRING_LITERAL,
    NULL_LITERAL,

    PAREN_OPEN,
    PAREN_CLOSE,
    BRACE_OPEN,
    BRACE_CLOSE,
    BRACKET_OPEN,
    BRACKET_CLOSE,
    DOT,
    COMMA,
    PLUS,
    MINUS,
    ASTERISK,
    SLASH,
    PERCENT,
    LESS_THAN,
    GREATER_THAN,
    EQUALS,

    LESS_THAN_EQUAL,
    GREATER_THAN_EQUAL,
    DOUBLE_EQUAL,
    NOT_EQUAL,

    KEYWORD_VAR,
    KEYWORD_FUN,
    KEYWORD_LOOP,
    KEYWORD_WHILE,
    KEYWORD_TICK,
    KEYWORD_CONT,
    KEYWORD_BREAK,
    KEYWORD_RETURN,
    KEYWORD_AND,
    KEYWORD_OR,
    KEYWORD_NOT,
    KEYWORD_IF,
    KEYWORD_ELSE,

    END_OF_FILE

}

data class BigtonToken(
    val type: BigtonTokenType,
    val content: String,
    val source: BigtonSource
)