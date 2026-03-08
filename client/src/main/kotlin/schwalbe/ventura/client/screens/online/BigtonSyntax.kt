
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.utils.parseHexColor
import schwalbe.ventura.engine.ui.Span
import schwalbe.ventura.engine.ui.Font
import schwalbe.ventura.client.screens.jetbrainsMonoI
import schwalbe.ventura.client.screens.jetbrainsMonoEB
import org.joml.Vector4fc

private val WHITE: Vector4fc = parseHexColor("fffdec")
private val GRAY: Vector4fc = parseHexColor("a1a3ad")
private val PINK: Vector4fc = parseHexColor("f95fa3")
private val PURPLE: Vector4fc = parseHexColor("8d65d4")
private val MAGENTA: Vector4fc = parseHexColor("d0a8ff")
private val ORANGE: Vector4fc = parseHexColor("f8634f")
private val CYAN: Vector4fc = parseHexColor("5cd5dc")
private val GREEN: Vector4fc = parseHexColor("56b49e")
private val YELLOW: Vector4fc = parseHexColor("d0bf69")

private val BASE_COLOR: Vector4fc = WHITE
private val COMMENT_COLOR: Vector4fc = GRAY
private val KEYWORD_COLOR: Vector4fc = PINK
private val NULL_LITERAL_COLOR: Vector4fc = CYAN
private val NUMBER_LITERAL_COLOR: Vector4fc = YELLOW
private val STRING_LITERAL_COLOR: Vector4fc = ORANGE
private val FUNCTION_CALL_COLOR: Vector4fc = GREEN

private val Char.isAsciiWhitespace: Boolean
    get() = when (this) {
        ' ', '\n', '\r', '\t' -> true
        else -> false
    }

private val Char.isAsciiAlphabetic: Boolean
    get() = this in 'a'..'z' || this in 'A'..'Z'

private val Char.isAsciiNumeric: Boolean
    get() = this in '0'..'9'

private val BIGTON_KEYWORDS: Set<String> = setOf(
    "var", "fun", "loop", "while", "tick", "continue", "break", "return",
    "and", "or", "not", "if", "else"
)

fun syntaxHighlightBigton(source: String): List<Span> {
    val spans = mutableListOf<Span>()
    var currentIdx: Int = 0
    val run = StringBuilder()
    fun hasCurrent(): Boolean = currentIdx < source.length
    fun getCurrent(): Char = source[currentIdx]
    fun advance() { currentIdx += 1 }
    fun collectWhile(cond: (Char) -> Boolean) {
        while (hasCurrent()) {
            val current: Char = getCurrent()
            if (!cond(current)) { break }
            run.append(current)
            advance()
        }
    }
    fun completeRun(): String {
        val result: String = run.toString()
        run.clear()
        return result
    }
    while (hasCurrent()) {
        val start: Char = getCurrent()
        if (start.isAsciiWhitespace) {
            collectWhile { it.isAsciiWhitespace }
            spans.add(Span(completeRun()))
            continue
        }
        if (start.isAsciiAlphabetic) {
            collectWhile {
                it.isAsciiAlphabetic || it.isAsciiNumeric || it == '_'
            }
            val word = completeRun()
            collectWhile { it.isAsciiWhitespace }
            val whitespace = completeRun()
            val isKeyword: Boolean = word in BIGTON_KEYWORDS
            val isCall: Boolean = hasCurrent() && getCurrent() == '('
            val color: Vector4fc? = when {
                isKeyword -> KEYWORD_COLOR
                word == "null" -> NULL_LITERAL_COLOR
                isCall -> FUNCTION_CALL_COLOR
                else -> null
            }
            val font: Font? = if (isKeyword) jetbrainsMonoEB() else null
            spans.add(Span(word, color, font))
            if (whitespace.isNotEmpty()) { spans.add(Span(whitespace)) }
            continue
        }
        if (start.isAsciiNumeric || start == '.') {
            collectWhile { it.isAsciiNumeric || it == '_' || it == '.' }
            val fragment = completeRun()
            val color: Vector4fc? =
                if (fragment.any { it.isAsciiNumeric }) NUMBER_LITERAL_COLOR
                else null
            spans.add(Span(fragment, color))
            continue
        }
        if (start == '\"') {
            run.append(start)
            advance()
            var isEscaped = false
            while (hasCurrent()) {
                val current = getCurrent()
                run.append(current)
                advance()
                if (!isEscaped && current == '\"') {
                    break
                }
                isEscaped = current == '\\'
            }
            spans.add(Span(completeRun(), color = STRING_LITERAL_COLOR))
            continue
        }
        if (start == '#') {
            collectWhile { it != '\n' }
            spans.add(Span(
                completeRun(), color = COMMENT_COLOR, font = jetbrainsMonoI()
            ))
            continue
        }
        spans.add(Span(start.toString(), color = BASE_COLOR))
        advance()
    }
    return spans
}
