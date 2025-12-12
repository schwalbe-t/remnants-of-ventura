
package schwalbe.ventura.bigton.compilation

import schwalbe.ventura.bigton.*

class BigtonParser(val tokens: List<BigtonToken>) {

    fun tokenAt(index: Int): BigtonToken
        = tokens.getOrNull(index)
        ?: BigtonToken(
            BigtonTokenType.END_OF_FILE, "",
            tokens.lastOrNull()?.line ?: 0
        )
    
    var currIdx: Int = 0
    var curr: BigtonToken = this.tokenAt(this.currIdx)

    fun advance() {
        this.currIdx += 1
        this.curr = this.tokenAt(this.currIdx)
    }

}

val unaryBindingPower: Map<BigtonTokenType, Int> = mapOf(
    BigtonTokenType.ASTERISK    to 6,
    BigtonTokenType.MINUS       to 6,

    BigtonTokenType.KEYWORD_NOT to 2
)

val binaryBindingPower: Map<BigtonTokenType, Int> = mapOf(
    BigtonTokenType.PAREN_OPEN          to 7,
    BigtonTokenType.DOT                 to 7,

    BigtonTokenType.ASTERISK            to 5,
    BigtonTokenType.SLASH               to 5,
    BigtonTokenType.PERCENT             to 5,
    
    BigtonTokenType.PLUS                to 4,
    BigtonTokenType.MINUS               to 4,
    
    BigtonTokenType.LESS_THAN           to 3,
    BigtonTokenType.GREATER_THAN        to 3,
    BigtonTokenType.LESS_THAN_EQUAL     to 3,
    BigtonTokenType.GREATER_THAN_EQUAL  to 3,
    BigtonTokenType.DOUBLE_EQUAL        to 3,
    BigtonTokenType.NOT_EQUAL           to 3,

    BigtonTokenType.KEYWORD_AND         to 1,
    BigtonTokenType.KEYWORD_OR          to 1
)

fun BigtonParser.parseValue(): BigtonAst {
    val start: BigtonToken = this.curr
    when (start.type) {
        BigtonTokenType.IDENTIFIER,
        BigtonTokenType.INT_LITERAL,
        BigtonTokenType.FLOAT_LITERAL,
        BigtonTokenType.STRING_LITERAL,
        BigtonTokenType.NULL_LITERAL -> {
            this.advance()
            val astType: BigtonAstType = when(start.type) {
                BigtonTokenType.IDENTIFIER      -> BigtonAstType.IDENTIFIER
                BigtonTokenType.INT_LITERAL     -> BigtonAstType.INT_LITERAL
                BigtonTokenType.FLOAT_LITERAL   -> BigtonAstType.FLOAT_LITERAL
                BigtonTokenType.STRING_LITERAL  -> BigtonAstType.STRING_LITERAL
                BigtonTokenType.NULL_LITERAL    -> BigtonAstType.NULL_LITERAL
                else -> throw Exception("unreachable")
            }
            return BigtonAst(astType, start.line, start.content)
        }
        BigtonTokenType.PAREN_OPEN -> {
            this.advance()
            val elems = mutableListOf<BigtonAst>()
            while (this.curr.type != BigtonTokenType.PAREN_CLOSE) {
                elems.add(this.parseExpression())
                if (this.curr.type == BigtonTokenType.COMMA) {
                    this.advance()
                } else if (this.curr.type != BigtonTokenType.PAREN_CLOSE) {
                    throw BigtonException(
                        BigtonErrorType.MISSING_EXPECTED_COMMA, this.curr.line
                    )
                }
            }
            this.advance()
            return when (elems.size) {
                0 -> throw BigtonException(
                    BigtonErrorType.EMPTY_PARENTHESES, start.line
                )
                1 -> elems[0]
                else -> BigtonAst(
                    BigtonAstType.TUPLE_LITERAL, start.line, null, elems
                )
            }
        }
        BigtonTokenType.BRACE_OPEN -> {
            this.advance()
            val memVals = mutableListOf<BigtonAst>()
            val memNames = mutableListOf<String>()
            while (this.curr.type != BigtonTokenType.BRACE_CLOSE) {
                if (this.curr.type != BigtonTokenType.IDENTIFIER) {
                    throw BigtonException(
                        BigtonErrorType.MISSING_EXPECTED_MEMBER_NAME,
                        this.curr.line
                    )
                }
                memNames.add(this.curr.content)
                this.advance()
                if (this.curr.type != BigtonTokenType.EQUALS) {
                    throw BigtonException(
                        BigtonErrorType.MISSING_EXPECTED_MEMBER_EQUALS,
                        this.curr.line
                    )
                }
                this.advance()
                memVals.add(this.parseExpression())
                if (this.curr.type == BigtonTokenType.COMMA) {
                    this.advance()
                } else if (this.curr.type != BigtonTokenType.BRACE_CLOSE) {
                    throw BigtonException(
                        BigtonErrorType.MISSING_EXPECTED_COMMA, this.curr.line
                    )
                }
            }
            this.advance()
            return BigtonAst(
                BigtonAstType.OBJECT_LITERAL, start.line, memNames, memVals
            )
        }
        else -> {}
    }
    val op: BigtonToken = this.curr
    val currPower: Int? = unaryBindingPower[op.type]
    if (currPower == null) {
        throw BigtonException(
            BigtonErrorType.MISSING_EXPECTED_UNARY_OR_VALUE, op.line
        )
    }
    this.advance()
    val operand: BigtonAst = this.parseExpression(currPower)
    val astType: BigtonAstType = when (op.type) {
        BigtonTokenType.ASTERISK    -> BigtonAstType.DEREF
        BigtonTokenType.MINUS       -> BigtonAstType.NEGATE
        BigtonTokenType.KEYWORD_NOT -> BigtonAstType.NOT
        else -> throw java.lang.UnsupportedOperationException(
            "Unary operator token '${op.type.name}' present in binding "
                + "power table, but not implemented here"
        )
    }
    return BigtonAst(astType, op.line, null, listOf(operand))
}

fun BigtonParser.parseExpression(parentPower: Int = 0): BigtonAst {
    var acc: BigtonAst = this.parseValue()
    while (true) {
        val op: BigtonToken = this.curr
        val currPower: Int? = binaryBindingPower[op.type]
        if (currPower == null || currPower <= parentPower) { return acc }
        this.advance()
        // special binary operators (special / fixed rhs)
        when (op.type) {
            BigtonTokenType.PAREN_OPEN -> {
                val called: String = when (acc.type) {
                    BigtonAstType.IDENTIFIER -> acc.castArg<String>()
                    else -> throw BigtonException(
                        BigtonErrorType.CALLING_EXPRESSION, op.line
                    )
                }
                val args = mutableListOf<BigtonAst>()
                while (this.curr.type != BigtonTokenType.PAREN_CLOSE) {
                    args.add(this.parseExpression())
                    if (this.curr.type == BigtonTokenType.COMMA) {
                        this.advance()
                    } else if (this.curr.type != BigtonTokenType.PAREN_CLOSE) {
                        throw BigtonException(
                            BigtonErrorType.MISSING_EXPECTED_COMMA,
                            this.curr.line
                        )
                    }
                }
                this.advance()
                acc = BigtonAst(BigtonAstType.CALL, op.line, called, args)
                continue
            }
            BigtonTokenType.DOT -> {
                acc = when (this.curr.type) {
                    BigtonTokenType.INT_LITERAL -> BigtonAst(
                        BigtonAstType.TUPLE_MEMBER, op.line,
                        this.curr.content.toInt(), listOf(acc)
                    )
                    BigtonTokenType.IDENTIFIER -> BigtonAst(
                        BigtonAstType.OBJECT_MEMBER, op.line,
                        this.curr.content, listOf(acc)
                    )
                    else -> throw BigtonException(
                        BigtonErrorType.INVALID_MEMBER_SYNTAX, this.curr.line
                    )
                }
                this.advance()
                continue
            }
            else -> {}
        }
        // normal binary operators (free rhs)
        val lhs: BigtonAst = acc
        val rhs: BigtonAst = this.parseExpression(currPower)
        val astType: BigtonAstType = when (op.type) {
            BigtonTokenType.ASTERISK            -> BigtonAstType.MULTIPLY
            BigtonTokenType.SLASH               -> BigtonAstType.DIVIDE
            BigtonTokenType.PERCENT             -> BigtonAstType.REMAINDER
            BigtonTokenType.PLUS                -> BigtonAstType.ADD
            BigtonTokenType.MINUS               -> BigtonAstType.SUBTRACT
            BigtonTokenType.LESS_THAN           -> BigtonAstType.LESS_THAN
            BigtonTokenType.GREATER_THAN        -> BigtonAstType.GREATER_THAN
            BigtonTokenType.LESS_THAN_EQUAL     -> BigtonAstType.LESS_THAN_EQUAL
            BigtonTokenType.GREATER_THAN_EQUAL  -> BigtonAstType.GREATER_THAN_EQUAL
            BigtonTokenType.DOUBLE_EQUAL        -> BigtonAstType.EQUAL
            BigtonTokenType.NOT_EQUAL           -> BigtonAstType.NOT_EQUAL
            BigtonTokenType.KEYWORD_AND         -> BigtonAstType.AND
            BigtonTokenType.KEYWORD_OR          -> BigtonAstType.OR
            else -> throw java.lang.UnsupportedOperationException(
                "Binary operator token '${op.type.name}' present in binding "
                    + "power table, but not implemented here"
            )
        }
        acc = BigtonAst(astType, op.line, null, listOf(lhs, rhs))
    }
}

fun BigtonParser.parseStatement(): BigtonAst {
    val start: BigtonToken = this.curr
    when (start.type) {
        BigtonTokenType.KEYWORD_CONT -> {
            this.advance()
            return BigtonAst(BigtonAstType.CONTINUE, start.line)
        }
        BigtonTokenType.KEYWORD_BREAK -> {
            this.advance()
            return BigtonAst(BigtonAstType.BREAK, start.line)
        }
        BigtonTokenType.KEYWORD_RETURN -> {
            this.advance()
            val v: BigtonAst = this.parseExpression()
            return BigtonAst(BigtonAstType.RETURN, start.line, null, listOf(v))
        }
        BigtonTokenType.KEYWORD_LOOP -> {
            this.advance()
            val body: List<BigtonAst> = this.parseBracedStatementList()
            return BigtonAst(BigtonAstType.LOOP, start.line, body)
        }
        BigtonTokenType.KEYWORD_TICK -> {
            this.advance()
            val body: List<BigtonAst> = this.parseBracedStatementList()
            return BigtonAst(BigtonAstType.TICK, start.line, body)
        }
        BigtonTokenType.KEYWORD_WHILE -> {
            this.advance()
            val cond: BigtonAst = this.parseExpression()
            val body: List<BigtonAst> = this.parseBracedStatementList()
            return BigtonAst(
                BigtonAstType.WHILE, start.line, body, listOf(cond)
            )
        }
        BigtonTokenType.KEYWORD_VAR -> {
            this.advance()
            if (this.curr.type != BigtonTokenType.IDENTIFIER) {
                throw BigtonException(
                    BigtonErrorType.MISSING_EXPECTED_VARIABLE_NAME,
                    this.curr.line
                )
            }
            val name: String = this.curr.content
            this.advance()
            if (this.curr.type != BigtonTokenType.EQUALS) {
                throw BigtonException(
                    BigtonErrorType.MISSING_EXPECTED_VAR_EQUALS, this.curr.line
                )
            }
            this.advance()
            val value: BigtonAst = this.parseExpression()
            return BigtonAst(
                BigtonAstType.VARIABLE, start.line, name, listOf(value)
            )
        }
        BigtonTokenType.KEYWORD_IF -> {
            this.advance()
            val cond: BigtonAst = this.parseExpression()
            val if_body: List<BigtonAst> = this.parseBracedStatementList()
            val else_body: List<BigtonAst>? = when {
                this.curr.type == BigtonTokenType.KEYWORD_ELSE -> {
                    this.advance()
                    if (this.curr.type == BigtonTokenType.KEYWORD_IF) {
                        listOf(this.parseStatement())
                    } else {
                        this.parseBracedStatementList()
                    }
                }
                else -> null
            }
            val branches: Pair<List<BigtonAst>, List<BigtonAst>?>
                = Pair(if_body, else_body)
            return BigtonAst(
                BigtonAstType.IF, start.line, branches, listOf(cond)
            )
        }
        BigtonTokenType.KEYWORD_FUN -> {
            this.advance()
            if (this.curr.type != BigtonTokenType.IDENTIFIER) {
                throw BigtonException(
                    BigtonErrorType.MISSING_EXPECTED_FUNCTION_NAME,
                    this.curr.line
                )
            }
            val name: String = this.curr.content
            this.advance()
            if (this.curr.type != BigtonTokenType.PAREN_OPEN) {
                throw BigtonException(
                    BigtonErrorType.MISSING_EXPECTED_FUNC_ARGS_OPEN,
                    this.curr.line
                )
            }
            this.advance()
            val argNames = mutableListOf<String>()
            while (this.curr.type != BigtonTokenType.PAREN_CLOSE) {
                if (this.curr.type != BigtonTokenType.IDENTIFIER) {
                    throw BigtonException(
                        BigtonErrorType.MISSING_EXPECTED_ARGUMENT_NAME,
                        this.curr.line
                    )
                }
                argNames.add(this.curr.content)
                this.advance()
                if (this.curr.type == BigtonTokenType.COMMA) {
                    this.advance()
                } else if (this.curr.type != BigtonTokenType.PAREN_CLOSE) {
                    throw BigtonException(
                        BigtonErrorType.MISSING_EXPECTED_COMMA,
                        this.curr.line
                    )
                }
            }
            this.advance()
            val body: List<BigtonAst> = this.parseBracedStatementList()
            return BigtonAst(
                BigtonAstType.FUNCTION, start.line,
                BigtonAstFunction(name, argNames, body)
            )
        }
        else -> {}
    }
    val lhs: BigtonAst = this.parseExpression()
    if (this.curr.type != BigtonTokenType.EQUALS) { return lhs }
    val op: BigtonToken = this.curr
    this.advance()
    val rhs: BigtonAst = this.parseExpression()
    return BigtonAst(BigtonAstType.ASSIGNMENT, op.line, null, listOf(lhs, rhs))
}

fun BigtonParser.parseBracedStatementList(): List<BigtonAst> {
    if (this.curr.type != BigtonTokenType.BRACE_OPEN) {
        throw BigtonException(
            BigtonErrorType.MISSING_EXPECTED_OPENING_BRACE, this.curr.line
        )
    }
    this.advance()
    val statements: List<BigtonAst> = this.parseStatementList()
    if (this.curr.type != BigtonTokenType.BRACE_CLOSE) {
        throw BigtonException(
            BigtonErrorType.MISSING_EXPECTED_CLOSING_BRACE, this.curr.line
        )
    }
    this.advance()
    return statements
}

fun BigtonParser.parseStatementList(): List<BigtonAst> {
    val statements = mutableListOf<BigtonAst>()
    while (true) {
        when (this.curr.type) {
            BigtonTokenType.BRACE_CLOSE,
            BigtonTokenType.END_OF_FILE -> break
            else -> {}
        }
        statements.add(this.parseStatement())
    }
    return statements
}