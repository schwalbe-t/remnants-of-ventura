
package schwalbe.ventura.bigton.compilation

import schwalbe.ventura.bigton.*

private fun isAsciiWhitespace(ch: Char): Boolean = when (ch) {
    ' ', '\n', '\r', '\t' -> true
    else -> false
}

private fun isAsciiAlphabetic(ch: Char): Boolean
    = ch in 'a'..'z' || ch in 'A'..'Z'

private fun isAsciiNumeric(ch: Char): Boolean
    = ch in '0'..'9'

private fun isAsciiAlphanumeric(ch: Char): Boolean
    = isAsciiAlphabetic(ch) || isAsciiNumeric(ch)

// Kotlin VS Code extension doesn't properly support char literals
// and opens a string literal when one writes '\"'
private const val DOUBLE_QUOTES: Char = "\""[0]

fun tokenize(file: String, source: String): List<BigtonToken> {
    var currentIdx: Int = 0
    var currentLine: Int = 0
    val result = mutableListOf<BigtonToken>()
    fun advToken(type: BigtonTokenType, content: String): Boolean {
        result.add(BigtonToken(type, content, BigtonSource(currentLine, file)))
        currentIdx += content.length
        return true
    }
    fun addToken(type: BigtonTokenType, content: String): Boolean {
        result.add(BigtonToken(type, content, BigtonSource(currentLine, file)))
        return true
    }
    while (currentIdx < source.length) {
        val current: Char = source[currentIdx]
        val next: Char = source.elementAtOrNull(currentIdx + 1) ?: 0.toChar()
        if (isAsciiWhitespace(current)) {
            when {
                current == '\r' && next == '\n' -> {
                    currentLine += 1
                    currentIdx += 2
                }
                current == '\r' || current == '\n' -> {
                    currentLine += 1
                    currentIdx += 1
                }
                else -> currentIdx += 1
            }
            continue
        }
        if (isAsciiAlphabetic(current) || current == '_') {
            val startIdx: Int = currentIdx
            while (currentIdx < source.length) {
                val c: Char = source[currentIdx]
                if (!isAsciiAlphanumeric(c) && c != '_') { break }
                currentIdx += 1
            }
            val content: String = source.substring(startIdx, currentIdx)
            when (content) {
                "var"      -> addToken(BigtonTokenType.KEYWORD_VAR,    content)
                "fun"      -> addToken(BigtonTokenType.KEYWORD_FUN,    content)
                "loop"     -> addToken(BigtonTokenType.KEYWORD_LOOP,   content)
                "while"    -> addToken(BigtonTokenType.KEYWORD_WHILE,  content)
                "tick"     -> addToken(BigtonTokenType.KEYWORD_TICK,   content)
                "continue" -> addToken(BigtonTokenType.KEYWORD_CONT,   content)
                "break"    -> addToken(BigtonTokenType.KEYWORD_BREAK,  content)
                "return"   -> addToken(BigtonTokenType.KEYWORD_RETURN, content)
                "and"      -> addToken(BigtonTokenType.KEYWORD_AND,    content)
                "or"       -> addToken(BigtonTokenType.KEYWORD_OR,     content)
                "not"      -> addToken(BigtonTokenType.KEYWORD_NOT,    content)
                "if"       -> addToken(BigtonTokenType.KEYWORD_IF,     content)
                "else"     -> addToken(BigtonTokenType.KEYWORD_ELSE,   content)
                "null"     -> addToken(BigtonTokenType.NULL_LITERAL,   content)
                else       -> addToken(BigtonTokenType.IDENTIFIER,     content)
            }
            continue
        }
        if (isAsciiNumeric(current)) {
            val startIdx: Int = currentIdx
            var hadDot: Boolean = false
            while (currentIdx < source.length) {
                val c: Char = source[currentIdx]
                if (!isAsciiNumeric(c) && c != '_' && c != '.') { break }
                if (c == '.') {
                    if (hadDot) {
                        throw BigtonException(
                            BigtonErrorType.MULTIPLE_DOTS_IN_NUMERIC,
                            BigtonSource(currentLine, file)
                        )
                    }
                    hadDot = true
                }
                hadDot = hadDot || c == '.'
                currentIdx += 1
            }
            val tokenType: BigtonTokenType =
                if (hadDot) { BigtonTokenType.FLOAT_LITERAL }
                else { BigtonTokenType.INT_LITERAL }
            val content: String = source.substring(startIdx, currentIdx)
                .replace("_", "")
            addToken(tokenType, content)
            continue
        }
        if (current == DOUBLE_QUOTES) {
            currentIdx += 1
            val content = StringBuilder()
            var escaped: Boolean = false
            while (true) {
                if (currentIdx >= source.length) {
                    throw BigtonException(
                        BigtonErrorType.UNCLOSED_STRING_LITERAL,
                        BigtonSource(currentLine, file)
                    )
                }
                val c: Char = source[currentIdx]
                val n: Char = source.elementAtOrNull(currentIdx + 1)
                    ?: 0.toChar()
                if (c == '\r' && n == '\n') {
                    currentLine += 1
                    currentIdx += 2
                    if (!escaped) { content.append("\r\n") }
                    escaped = false
                } else if (c == '\r' || c == '\n') {
                    currentLine += 1
                    currentIdx += 1
                    if (!escaped) { content.append(c) }
                    escaped = false
                } else if (escaped) {
                    when (c) {
                        DOUBLE_QUOTES -> content.append(DOUBLE_QUOTES)
                        '\'' -> content.append('\'')
                        '\\' -> content.append('\\')
                        'n'  -> content.append('\n')
                        'r'  -> content.append('\r')
                        'b'  -> content.append('\b')
                        't'  -> content.append('\t')
                        else -> content.append(c)
                    }
                    escaped = false
                    currentIdx += 1
                } else if (c == DOUBLE_QUOTES) {
                    currentIdx += 1
                    break
                } else if (c == '\\') {
                    escaped = true
                    currentIdx += 1
                } else {
                    content.append(c)
                    currentIdx += 1
                }
            }
            addToken(BigtonTokenType.STRING_LITERAL, content.toString())         
            continue
        }
        val isDouble: Boolean = when (current to next) {
            '<' to '=' -> advToken(BigtonTokenType.LESS_THAN_EQUAL,    "<=")
            '>' to '=' -> advToken(BigtonTokenType.GREATER_THAN_EQUAL, ">=")
            '=' to '=' -> advToken(BigtonTokenType.DOUBLE_EQUAL,       "==")
            '!' to '=' -> advToken(BigtonTokenType.NOT_EQUAL,          "!=")
            else -> false
        }
        if (isDouble) { continue }
        val isSingle: Boolean = when (current) {
            '(' -> advToken(BigtonTokenType.PAREN_OPEN,     "(")
            ')' -> advToken(BigtonTokenType.PAREN_CLOSE,    ")")
            '{' -> advToken(BigtonTokenType.BRACE_OPEN,     "{")
            '}' -> advToken(BigtonTokenType.BRACE_CLOSE,    "}")
            '.' -> advToken(BigtonTokenType.DOT,            ".")
            ',' -> advToken(BigtonTokenType.COMMA,          ",")
            '+' -> advToken(BigtonTokenType.PLUS,           "+")
            '-' -> advToken(BigtonTokenType.MINUS,          "-")
            '*' -> advToken(BigtonTokenType.ASTERISK,       "*")
            '/' -> advToken(BigtonTokenType.SLASH,          "/")
            '%' -> advToken(BigtonTokenType.PERCENT,        "%")
            '<' -> advToken(BigtonTokenType.LESS_THAN,      "<")
            '>' -> advToken(BigtonTokenType.GREATER_THAN,   ">")
            '=' -> advToken(BigtonTokenType.EQUALS,         "=")
            '@' -> advToken(BigtonTokenType.AT,             "@")
            '#' -> {
                while (currentIdx < source.length) {
                    val c: Char = source[currentIdx]
                    if (c == '\n' || c == '\r') { break }
                    currentIdx += 1
                }
                true
            }
            else -> false
        }
        if (isSingle) { continue }
        throw BigtonException(
            BigtonErrorType.INVALID_TOKEN, BigtonSource(currentLine, file)
        )
    }
    return result
}