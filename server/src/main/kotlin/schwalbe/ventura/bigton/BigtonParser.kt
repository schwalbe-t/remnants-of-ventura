
package schwalbe.ventura.bigton

class BigtonParser(val tokens: List<BigtonToken>) {

    fun tokenAt(index: Int): BigtonToken
        = tokens.getOrNull(index)
        ?: BigtonToken(
            BigtonTokenType.END_OF_FILE, "",
            tokens.lastOrNull()?.source ?: BigtonSource(0, "<unknown>")
        )
    
    var currIdx: Int = 0
    var curr: BigtonToken = this.tokenAt(this.currIdx)

    fun advance() {
        this.currIdx += 1
        this.curr = this.tokenAt(this.currIdx)
    }

}

val unaryBindingPower: Map<BigtonTokenType, Int> = mapOf(
    BigtonTokenType.MINUS       to 6,

    BigtonTokenType.KEYWORD_NOT to 2
)

val binaryBindingPower: Map<BigtonTokenType, Int> = mapOf(
    BigtonTokenType.PAREN_OPEN          to 7,
    BigtonTokenType.DOT                 to 7,
    BigtonTokenType.BRACKET_OPEN        to 7,

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

private fun BigtonParser.parseValueList(end: BigtonTokenType): List<BigtonAst> {
    val elems = mutableListOf<BigtonAst>()
    while (this.curr.type != end) {
        elems.add(this.parseExpression())
        if (this.curr.type == BigtonTokenType.COMMA) {
            this.advance()
        } else if (this.curr.type != end) {
            throw BigtonException(
                BigtonErrorType.MISSING_EXPECTED_COMMA,
                this.curr.source
            )
        }
    }
    return elems
}

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
            return BigtonAst(astType, start.source, start.content)
        }
        BigtonTokenType.PAREN_OPEN -> {
            this.advance()
            val elems = this.parseValueList(BigtonTokenType.PAREN_CLOSE)
            this.advance()
            return when (elems.size) {
                0 -> throw BigtonException(
                    BigtonErrorType.EMPTY_PARENTHESES, start.source
                )
                1 -> elems[0]
                else -> BigtonAst(
                    BigtonAstType.TUPLE_LITERAL, start.source, null, elems
                )
            }
        }
        BigtonTokenType.BRACKET_OPEN -> {
            this.advance()
            val elems = this.parseValueList(BigtonTokenType.BRACKET_CLOSE)
            this.advance()
            return BigtonAst(
                BigtonAstType.ARRAY_LITERAL, start.source, null, elems
            )
        }
        BigtonTokenType.BRACE_OPEN -> {
            this.advance()
            val memVals = mutableListOf<BigtonAst>()
            val memNames = mutableListOf<String>()
            while (this.curr.type != BigtonTokenType.BRACE_CLOSE) {
                if (this.curr.type != BigtonTokenType.IDENTIFIER) {
                    throw BigtonException(
                        BigtonErrorType.MISSING_EXPECTED_MEMBER_NAME,
                        this.curr.source
                    )
                }
                memNames.add(this.curr.content)
                this.advance()
                if (this.curr.type != BigtonTokenType.EQUALS) {
                    throw BigtonException(
                        BigtonErrorType.MISSING_EXPECTED_MEMBER_EQUALS,
                        this.curr.source
                    )
                }
                this.advance()
                memVals.add(this.parseExpression())
                if (this.curr.type == BigtonTokenType.COMMA) {
                    this.advance()
                } else if (this.curr.type != BigtonTokenType.BRACE_CLOSE) {
                    throw BigtonException(
                        BigtonErrorType.MISSING_EXPECTED_COMMA, this.curr.source
                    )
                }
            }
            this.advance()
            return BigtonAst(
                BigtonAstType.OBJECT_LITERAL, start.source, memNames, memVals
            )
        }
        else -> {}
    }
    val op: BigtonToken = this.curr
    val currPower: Int = unaryBindingPower[op.type]
        ?: throw BigtonException(
            BigtonErrorType.MISSING_EXPECTED_UNARY_OR_VALUE, op.source
        )
    this.advance()
    val operand: BigtonAst = this.parseExpression(currPower)
    val astType: BigtonAstType = when (op.type) {
        BigtonTokenType.MINUS       -> BigtonAstType.NEGATE
        BigtonTokenType.KEYWORD_NOT -> BigtonAstType.NOT
        else -> throw java.lang.UnsupportedOperationException(
            "Unary operator token '${op.type.name}' present in binding "
                + "power table, but not implemented here"
        )
    }
    return BigtonAst(astType, op.source, null, listOf(operand))
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
                val args: MutableList<BigtonAst> = mutableListOf()
                val called: String = when (acc.type) {
                    BigtonAstType.IDENTIFIER -> acc.castArg<String>()
                    BigtonAstType.OBJECT_MEMBER -> {
                        args.addAll(acc.children)
                        acc.castArg<String>()
                    }
                    else -> throw BigtonException(
                        BigtonErrorType.CALLING_EXPRESSION, op.source
                    )
                }
                args.addAll(this.parseValueList(BigtonTokenType.PAREN_CLOSE))
                this.advance()
                acc = BigtonAst(BigtonAstType.CALL, op.source, called, args)
                continue
            }
            BigtonTokenType.DOT -> {
                acc = when (this.curr.type) {
                    BigtonTokenType.INT_LITERAL -> BigtonAst(
                        BigtonAstType.TUPLE_MEMBER, op.source,
                        this.curr.content.toInt(), listOf(acc)
                    )
                    BigtonTokenType.IDENTIFIER -> BigtonAst(
                        BigtonAstType.OBJECT_MEMBER, op.source,
                        this.curr.content, listOf(acc)
                    )
                    else -> throw BigtonException(
                        BigtonErrorType.INVALID_MEMBER_SYNTAX, this.curr.source
                    )
                }
                this.advance()
                continue
            }
            BigtonTokenType.BRACKET_OPEN -> {
                val array: BigtonAst = acc
                val index: BigtonAst = this.parseExpression()
                if (this.curr.type != BigtonTokenType.BRACKET_CLOSE) {
                    throw BigtonException(
                        BigtonErrorType.MISSING_EXPECTED_CLOSING_BRACKET,
                        this.curr.source
                    )
                }
                this.advance()
                acc = BigtonAst(
                    BigtonAstType.ARRAY_INDEX, op.source, null,
                    listOf(array, index)
                )
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
        acc = BigtonAst(astType, op.source, null, listOf(lhs, rhs))
    }
}

fun BigtonParser.parseStatement(): BigtonAst {
    val start: BigtonToken = this.curr
    when (start.type) {
        BigtonTokenType.KEYWORD_CONT -> {
            this.advance()
            return BigtonAst(BigtonAstType.CONTINUE, start.source)
        }
        BigtonTokenType.KEYWORD_BREAK -> {
            this.advance()
            return BigtonAst(BigtonAstType.BREAK, start.source)
        }
        BigtonTokenType.KEYWORD_RETURN -> {
            this.advance()
            val v: BigtonAst = this.parseExpression()
            return BigtonAst(
                BigtonAstType.RETURN, start.source, null, listOf(v)
            )
        }
        BigtonTokenType.KEYWORD_LOOP -> {
            this.advance()
            val body: List<BigtonAst> = this.parseBracedStatementList()
            return BigtonAst(BigtonAstType.LOOP, start.source, body)
        }
        BigtonTokenType.KEYWORD_TICK -> {
            this.advance()
            val body: List<BigtonAst> = this.parseBracedStatementList()
            return BigtonAst(BigtonAstType.TICK, start.source, body)
        }
        BigtonTokenType.KEYWORD_WHILE -> {
            this.advance()
            val cond: BigtonAst = this.parseExpression()
            val body: List<BigtonAst> = this.parseBracedStatementList()
            return BigtonAst(
                BigtonAstType.WHILE, start.source, body, listOf(cond)
            )
        }
        BigtonTokenType.KEYWORD_VAR -> {
            this.advance()
            if (this.curr.type != BigtonTokenType.IDENTIFIER) {
                throw BigtonException(
                    BigtonErrorType.MISSING_EXPECTED_VARIABLE_NAME,
                    this.curr.source
                )
            }
            val name: String = this.curr.content
            this.advance()
            if (this.curr.type != BigtonTokenType.EQUALS) {
                throw BigtonException(
                    BigtonErrorType.MISSING_EXPECTED_VAR_EQUALS,
                    this.curr.source
                )
            }
            this.advance()
            val value: BigtonAst = this.parseExpression()
            return BigtonAst(
                BigtonAstType.VARIABLE, start.source, name, listOf(value)
            )
        }
        BigtonTokenType.KEYWORD_IF -> {
            this.advance()
            val cond: BigtonAst = this.parseExpression()
            val ifBody: List<BigtonAst> = this.parseBracedStatementList()
            val elseBody: List<BigtonAst>? = when {
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
                = Pair(ifBody, elseBody)
            return BigtonAst(
                BigtonAstType.IF, start.source, branches, listOf(cond)
            )
        }
        BigtonTokenType.KEYWORD_FUN -> {
            this.advance()
            if (this.curr.type != BigtonTokenType.IDENTIFIER) {
                throw BigtonException(
                    BigtonErrorType.MISSING_EXPECTED_FUNCTION_NAME,
                    this.curr.source
                )
            }
            val name: String = this.curr.content
            this.advance()
            if (this.curr.type != BigtonTokenType.PAREN_OPEN) {
                throw BigtonException(
                    BigtonErrorType.MISSING_EXPECTED_FUNC_ARGS_OPEN,
                    this.curr.source
                )
            }
            this.advance()
            val argNames = mutableListOf<String>()
            while (this.curr.type != BigtonTokenType.PAREN_CLOSE) {
                if (this.curr.type != BigtonTokenType.IDENTIFIER) {
                    throw BigtonException(
                        BigtonErrorType.MISSING_EXPECTED_ARGUMENT_NAME,
                        this.curr.source
                    )
                }
                argNames.add(this.curr.content)
                this.advance()
                if (this.curr.type == BigtonTokenType.COMMA) {
                    this.advance()
                } else if (this.curr.type != BigtonTokenType.PAREN_CLOSE) {
                    throw BigtonException(
                        BigtonErrorType.MISSING_EXPECTED_COMMA,
                        this.curr.source
                    )
                }
            }
            this.advance()
            val body: List<BigtonAst> = this.parseBracedStatementList()
            return BigtonAst(
                BigtonAstType.FUNCTION, start.source,
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
    return BigtonAst(
        BigtonAstType.ASSIGNMENT, op.source, null, listOf(lhs, rhs)
    )
}

fun BigtonParser.parseBracedStatementList(): List<BigtonAst> {
    if (this.curr.type != BigtonTokenType.BRACE_OPEN) {
        throw BigtonException(
            BigtonErrorType.MISSING_EXPECTED_OPENING_BRACE, this.curr.source
        )
    }
    this.advance()
    val statements: List<BigtonAst> = this.parseStatementList()
    if (this.curr.type != BigtonTokenType.BRACE_CLOSE) {
        throw BigtonException(
            BigtonErrorType.MISSING_EXPECTED_CLOSING_BRACE, this.curr.source
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