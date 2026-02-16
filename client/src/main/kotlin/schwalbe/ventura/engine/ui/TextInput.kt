
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.input.*
import org.joml.*
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import kotlin.math.roundToInt
import kotlin.streams.asSequence

private const val EOL: Int = 0x000A // = '\n' (end of line)
private const val SPACE: Int = 0x0020 // = ' ' (space)

private fun cpLineCount(value: List<Int>): Int
    = value.count { it == EOL } + 1

private fun cpLengthOfLine(value: List<Int>, lineIdx: Int): Int {
    var currLineIdx = 0
    var offset = 0
    while (offset < value.size) {
        var lineLen: Int = value.subList(offset, value.size).indexOf(EOL)
        if (lineLen == -1) { lineLen = value.size - offset }
        if (currLineIdx == lineIdx) { return lineLen }
        currLineIdx += 1
        offset += lineLen + 1
    }
    return 0 // out of bounds
}

private fun createSelection(a: Int, b: Int): IntRange? {
    if (a == b) { return null }
    return minOf(a, b)..maxOf(a, b)
}

class TextInput : GpuUiElement(), Focusable {
    
    companion object {
        val caretColor: Vector4fc = Vector4f(90f, 193f, 254f, 255f).div(255f)
        val selectColor: Vector4fc = Vector4f(90f, 193f, 254f, 80f).div(255f)
        const val CARET_WIDTH = 2
        val rightCaretPadding: UiSize = 5.vmin
        var caretScrollPadding: UiSize = 10.vmin
        const val CARET_BLINK_DELTA_MS = 750
    }
    
    data class Modification(
        val oldCaretPos: Int,
        val oldSelection: IntRange?,
        val edits: List<Edit>,
        val newCaretPos: Int,
        val newSelection: IntRange?
    ) {
        data class Edit(
            val isDeletion: Boolean,
            val atOffset: Int,
            val text: List<Int>
        )
    }

    class Caret {
        
        var posChanged: Boolean = true
        
        var line: Int = 0
            set(value) {
                if (field != value) {
                    this.posChanged = true
                    this.resetBlinking()
                }
                field = value
            }
        var column: Int = 0
            set(value) {
                if (field != value) {
                    this.posChanged = true
                    this.resetBlinking()
                }
                field = value
            }
        var offset: Int = 0
            private set
            
        private var lastBlinkTime: Long = 0
        var changedBlinkState: Boolean = false
        var blinkIsVisible: Boolean = false
            private set
            
        fun updateBlinkState() {
            val currentTime: Long = System.currentTimeMillis()
            val nextBlinkTime: Long = this.lastBlinkTime +
                TextInput.CARET_BLINK_DELTA_MS
            if (currentTime < nextBlinkTime) { return }
            this.changedBlinkState = true
            this.blinkIsVisible = !this.blinkIsVisible
            this.lastBlinkTime = currentTime
        }
        
        fun resetBlinking() {
            this.lastBlinkTime = System.currentTimeMillis()
            this.blinkIsVisible = true
            this.changedBlinkState = true
        }
            
        fun moveToOffset(value: List<Int>, offset: Int) {
            this.resetBlinking()
            this.line = 0
            this.column = 0
            for (cp in value.subList(0, offset)) {
                if (cp != EOL) {
                    this.column += 1
                } else {
                    this.line += 1
                    this.column = 0
                }
            }
            this.offset = offset
        }
        
        fun moveUp(value: List<Int>) {
            if (this.line >= 1) {
                this.line -= 1
            } else {
                this.column = 0
            }
            this.updateOffset(value)
        }
        
        fun moveDown(value: List<Int>) {
            if (this.line < cpLineCount(value) - 1) {
                this.line += 1
            } else {
                this.column = maxOf(
                    this.column, cpLengthOfLine(value, this.line)
                )
            }
            this.updateOffset(value)
        }
        
        fun moveLeft(value: List<Int>) {
            this.column = this.column
                .coerceIn(0, cpLengthOfLine(value, this.line))
            if (this.offset < 1) { return }
            val beforeCurr: Int = value[this.offset - 1]
            if (beforeCurr != EOL) {
                this.column -= 1
            } else {
                this.line -= 1
                this.column = cpLengthOfLine(value, this.line)
            }
            this.updateOffset(value)
        }
        
        fun moveRight(value: List<Int>) {
            this.column = this.column
                .coerceIn(0, cpLengthOfLine(value, this.line))
            this.moveToOffset(value, this.offset)
            if (this.offset >= value.size) { return }
            val afterCurr: Int = value[this.offset]
            if (afterCurr != EOL) {
                if (this.column == cpLengthOfLine(value, this.line)) { return }
                this.column += 1
            } else {
                this.line += 1
                this.column = 0
            }
            this.updateOffset(value)
        }
        
        fun updatedSelection(
            oldOffset: Int, oldSelection: IntRange?
        ): IntRange? {
            if (oldSelection == null) {
                return createSelection(oldOffset, this.offset)
            }
            val oldA: Int = oldSelection.first
            val oldB: Int = oldSelection.last
            val a: Int = if (oldOffset == oldA) { this.offset } else { oldA }
            val b: Int = if (oldOffset == oldB) { this.offset } else { oldB }
            return createSelection(a, b)
        }
            
        fun updateOffset(value: List<Int>) {
            this.offset = 0
            var currentLine = 0
            while (this.offset < value.size) {
                val remaining: Int = value.size - this.offset
                var lineLen = value
                    .subList(this.offset, value.size)
                    .indexOf(EOL)
                if (lineLen == -1) { lineLen = remaining }
                if (currentLine == this.line) {
                    this.offset += minOf(this.column, lineLen)
                    return
                }
                currentLine += 1
                this.offset += lineLen + 1
            }
            this.offset = value.size
        } 
    }
    
    
    var placeholder: Text? = null
        set(value) {
            field = value
            this.invalidate()
        }
        
    var content: Text? = null
        set(value) {
            field = value
            this.invalidate()
        }
        
    var isMultiline: Boolean = false
        set(value) {
            if (value != field) { this.invalidate() }
            field = value
        }
        
    var displayedValue: (List<Int>) -> List<Span>
        = { listOf(Span(it.joinToString("", transform = Character::toString))) }
        set(value) {
            field = value
            this.invalidate()
        }
        
    var onTypedText: (Int) -> Unit = { c -> this.writeText(c) }
        
    var onDeletedText: (Int) -> Unit = { c -> this.deleteLeft(1) }
        
    var tabLength: Int = 4
        
    private var actualValue: MutableList<Int> = mutableListOf()
    private var actualValueStr: String = ""
    var value: List<Int>
        get() = this.actualValue
        set(value) {
            this.actualValue = value.toMutableList()
            this.updateDisplayedValue()
        }
    val valueString: String
        get() = this.actualValueStr

    override val children: List<UiElement>
        get() = listOfNotNull(this.placeholder, this.content)
        
    var caret: Caret? = null
        private set
    var selection: IntRange? = null
        private set
    
    override fun onFocusLost() {
        this.caret = null
        this.invalidate()
    }
    
    private fun charIndexOf(caretPos: Int): Int
        = this.actualValueStr.offsetByCodePoints(0, caretPos)
    
    private fun caretPosOf(charIndex: Int): Int
        = if (this.actualValueStr.isEmpty()) { 0 }
        else { this.actualValueStr.codePointCount(0, charIndex) }
    
    private fun updateDisplayedValue() {
        this.actualValueStr = this.actualValue
            .joinToString("", transform = Character::toString)
        this.content?.withText(this.displayedValue(this.actualValue))
        this.invalidate()
    }
    
    private val edits: MutableList<Modification.Edit> = mutableListOf()
    
    private fun clearSelection() {
        val selection: IntRange = this.selection ?: return
        val deleted: MutableList<Int> = this.actualValue
            .subList(selection.first, selection.last)
        this.edits.add(Modification.Edit(
            isDeletion = true,
            atOffset = selection.first,
            text = deleted.toList()
        ))
        deleted.clear()
        this.selection = null
        this.caret?.moveToOffset(this.actualValue, selection.first)
        this.updateDisplayedValue()
    }
    
    private fun copySelected() {
        val selection: IntRange = this.selection ?: return
        val copied: String = this.actualValue
            .slice(selection.first..<selection.last)
            .joinToString(
                "", transform = Character::toString
            )
        Toolkit.getDefaultToolkit()
            .systemClipboard
            .setContents(StringSelection(copied), null)
    }
    
    private fun pasteClipboard() {
        val caret: Caret = this.caret ?: return
        val pasted: String
        try {
            pasted = Toolkit.getDefaultToolkit()
                .systemClipboard
                .getData(DataFlavor.stringFlavor)
                as? String ?: return
        } catch (ex: Exception) { return }
        this.clearSelection()
        this.writeText(pasted)
        this.updateDisplayedValue()
    }
    
    fun writeText(cp: Int) {
        val caret: Caret = this.caret ?: return
        this.actualValue.add(caret.offset, cp)
        this.edits.add(Modification.Edit(
            isDeletion = false,
            atOffset = caret.offset,
            text = listOf(cp)
        ))
        caret.moveRight(this.actualValue)
    }
    
    fun writeText(text: String) {
        for (cp in text.codePoints()) {
            this.writeText(cp)
        }
    }
    
    fun deleteLeft(n: Int) {
        val caret: Caret = this.caret ?: return
        val numRemoved: Int = minOf(n, caret.offset)
        this.edits.add(Modification.Edit(
            isDeletion = true,
            atOffset = caret.offset - numRemoved,
            text = this.actualValue
                .subList(caret.offset - numRemoved, caret.offset)
                .toList()
        ))
        for (i in 1..numRemoved) {
            caret.moveLeft(this.actualValue)
            this.actualValue.removeAt(caret.offset)
        }
    }
    
    fun applyMod(mod: Modification) {
        for (edit in mod.edits) {
            if (edit.isDeletion) {
                this.actualValue
                    .subList(edit.atOffset, edit.atOffset + edit.text.size)
                    .clear()
            } else {
                this.actualValue.addAll(edit.atOffset, edit.text)
            }
        }
        this.caret?.moveToOffset(this.actualValue, mod.newCaretPos)
        this.selection = mod.newSelection
        this.updateDisplayedValue()
    }
    
    fun undoMod(mod: Modification) {
        for (edit in mod.edits.reversed()) {
            if (edit.isDeletion) {
                this.actualValue.addAll(edit.atOffset, edit.text)
            } else {
                this.actualValue
                    .subList(edit.atOffset, edit.atOffset + edit.text.size)
                    .clear()
            }
        }
        this.caret?.moveToOffset(this.actualValue, mod.oldCaretPos)
        this.selection = mod.oldSelection
        this.updateDisplayedValue()
    }
    
    private fun moveCaretToCursor(context: UiElementContext) {
        val caret: Caret = this.caret ?: return
        val content: Text = this.content ?: return
        val relPosX: Int = Mouse.position.x().roundToInt() -
            context.absPxX
        val relPosY: Int = Mouse.position.y().roundToInt() -
            context.absPxY
        val lineHeight: Int = content.lineHeightPx.roundToInt()
        val line: Int = relPosY / lineHeight
        caret.line = line
        caret.column = this.caretPosOf(
            content.charIdxOfPos(relPosX.toFloat(), line) ?: 0
        )
        caret.updateOffset(this.actualValue)
    }
    
    private val history: MutableList<Modification> = mutableListOf()
    private val undoHistory: MutableList<Modification> = mutableListOf()
    
    override fun captureInput(context: UiElementContext) {
        val mouseInside: Boolean = Mouse.isInsideArea(
            context.visibleAbsLeft, context.visibleAbsTop,
            context.visibleAbsRight, context.visibleAbsBottom
        )
        val mouseLeftPressed: Boolean = context.global.nav.input
            .remainingOfType<MButtonDown>()
            .any { it.button == MButton.LEFT }
        if (this.caret == null && mouseInside && mouseLeftPressed) {
            context.global.currentlyInFocus = this
            this.caret = Caret()
            this.caret?.moveToOffset(this.actualValue, this.value.size)
            this.selection = null
            this.invalidate()
        }
        if (mouseInside) {
            Mouse.cursor = Cursor.IBEAM
        }
        if (this.caret != null && !mouseInside && mouseLeftPressed) {
            context.global.currentlyInFocus = null
        }
        val caret: Caret? = this.caret
        if (caret != null) {
            if (MButton.LEFT.isPressed) {
                val oldOffset: Int = caret.offset
                this.moveCaretToCursor(context)
                this.selection = caret
                    .updatedSelection(oldOffset, this.selection)
            }
            for (e in context.global.nav.input.remaining()) {
                val oldCaretPos: Int = caret.offset
                val oldSelection: IntRange? = this.selection
                this.edits.clear()
                when (e) {
                    is MButtonDown -> {
                        if (e.button != MButton.LEFT) { continue }
                        if (Key.LEFT_SHIFT.isPressed) { continue }
                        this.moveCaretToCursor(context)
                        this.selection = null
                        continue // do not remove event, don't record mod
                    }
                    is CharTyped -> {
                        this.clearSelection()
                        this.onTypedText(e.codepoint)
                        this.updateDisplayedValue()
                    }
                    is KeyDown, is KeyRepeat -> when {
                        e.key == Key.ENTER -> {
                            if (!this.isMultiline) { continue }
                            this.clearSelection()
                            this.onTypedText(EOL)
                            this.updateDisplayedValue()
                        }
                        e.key == Key.BACKSPACE -> {
                            val beforeCp: Int? = this.actualValue
                                .getOrNull(caret.offset - 1)
                            if (this.selection != null) {
                                this.clearSelection()
                                this.updateDisplayedValue()
                            } else if (beforeCp != null) {
                                this.onDeletedText(beforeCp)
                                this.updateDisplayedValue()
                            }
                        }
                        e.key == Key.TAB -> {
                            var lineStart: Int = this.actualValue
                                .subList(0, caret.offset)
                                .lastIndexOf(EOL)
                            if (lineStart == -1) { lineStart = 0 }
                            else { lineStart += 1 }
                            val lineCharIdx: Int = caret.offset - lineStart
                            var tabRemDist: Int = this.tabLength -
                                (lineCharIdx % this.tabLength)
                            for (i in 1..tabRemDist) {
                                val next: Int? = this.actualValue
                                    .getOrNull(caret.offset)
                                if (next != SPACE) {
                                    this.writeText(SPACE)
                                } else {
                                    caret.moveRight(this.actualValue)
                                }
                            }
                            this.updateDisplayedValue()
                        }
                        e.key == Key.LEFT -> {
                            val oldOffset: Int = caret.offset
                            caret.moveLeft(this.actualValue)
                            val selection: IntRange? = this.selection
                            if (Key.LEFT_SHIFT.isPressed) {
                                this.selection = caret
                                    .updatedSelection(oldOffset, selection)
                            } else if (selection != null) {
                                caret.moveToOffset(
                                    this.actualValue, selection.first
                                )
                                this.selection = null
                            }
                        }
                        e.key == Key.RIGHT -> {
                            val oldOffset: Int = caret.offset
                            caret.moveRight(this.actualValue)
                            val selection: IntRange? = this.selection
                            if (Key.LEFT_SHIFT.isPressed) {
                                this.selection = caret
                                    .updatedSelection(oldOffset, selection)
                            } else if (selection != null) {
                                caret.moveToOffset(
                                    this.actualValue, selection.last
                                )
                                this.selection = null
                            }
                        }
                        e.key == Key.UP -> {
                            val oldOffset: Int = caret.offset
                            caret.moveUp(this.actualValue)
                            if (Key.LEFT_SHIFT.isPressed) {
                                this.selection = caret.updatedSelection(
                                    oldOffset, this.selection
                                )
                            } else {
                                this.selection = null
                            }
                        }
                        e.key == Key.DOWN -> {
                            val oldOffset: Int = caret.offset
                            caret.moveDown(this.actualValue)
                            if (Key.LEFT_SHIFT.isPressed) {
                                this.selection = caret.updatedSelection(
                                    oldOffset, this.selection
                                )
                            } else {
                                this.selection = null
                            }
                        }
                        e.key == Key.A && Key.LEFT_CONTROL.isPressed -> {
                            this.selection = 0..this.actualValue.size
                            caret.moveToOffset(
                                this.actualValue, this.actualValue.size
                            )
                        }
                        e.key == Key.C && Key.LEFT_CONTROL.isPressed -> {
                            this.copySelected()
                        }
                        e.key == Key.X && Key.LEFT_CONTROL.isPressed -> {
                            this.copySelected()
                            this.clearSelection()
                        }
                        e.key == Key.V && Key.LEFT_CONTROL.isPressed -> {
                            this.pasteClipboard()
                        }
                        e.key == Key.Z && Key.LEFT_CONTROL.isPressed -> {
                            if (!Key.LEFT_CONTROL.isPressed) { continue }
                            if (Key.LEFT_SHIFT.isPressed) {
                                val mod: Modification = this.undoHistory
                                    .removeLastOrNull() ?: continue
                                this.history.add(mod)
                                this.applyMod(mod)
                            } else {
                                val mod: Modification = this.history
                                    .removeLastOrNull() ?: continue
                                this.undoHistory.add(mod)
                                this.undoMod(mod)
                            }
                            continue
                        }
                        e.key == Key.LEFT_CONTROL -> continue
                        e.key == Key.LEFT_SHIFT -> continue
                        e.key == Key.ESCAPE -> continue
                        else -> {
                            context.global.nav.input.remove(e)
                            continue
                        }
                    }
                    else -> continue
                }
                context.global.nav.input.remove(e)
                if (edits.isNotEmpty()) {
                    this.history.add(Modification(
                        oldCaretPos, oldSelection,
                        this.edits.toList(),
                        caret.offset, this.selection
                    ))
                    this.undoHistory.clear()
                }
            }
            caret.updateBlinkState()
            if (caret.changedBlinkState || caret.posChanged) {
                this.invalidate()
                caret.changedBlinkState = false
            }
        }
    }
    
    private fun requestCaretVisible(context: UiElementContext) {
        val scroll: Scroll = context.parent as? Scroll ?: return
        val padding: Int = TextInput.caretScrollPadding(context).roundToInt()
        scroll.requestVisible(this.caretX, this.caretY, padding)
    }
    
    override fun updateLayout(context: UiElementContext) {
        val content: Text? = this.content
        if (content != null) {
            content.withWrapping(false)
            content.withSize(0.px, 0.px)
            val caretPadding: Int = TextInput
                .rightCaretPadding(context).roundToInt()
            this.pxWidth = maxOf(this.pxWidth, content.pxWidth + caretPadding)
            this.pxHeight = maxOf(
                this.pxHeight,
                content.pxHeight + if (!this.isMultiline) { 0 }
                    else { context.parentPxHeight / 2 }
            )
        }
    }
    
    private var caretX: Int = 0
    private var caretY: Int = 0
    
    private fun computeCaretPosition(context: UiElementContext) {
        val caret: Caret = this.caret ?: return
        val content: Text = this.content ?: return
        val caretCharIdx: Int = this.charIndexOf(caret.offset)
        val beforeCaret: String = this.actualValueStr
            .substring(0, caretCharIdx)
        val caretLineIdx: Int = beforeCaret.count { it == '\n' }
        val lineCharIdx: Int = caretCharIdx -
            beforeCaret.lastIndexOf('\n') - 1
        val caretPos: Vector2f = content
            .posOfCharIdx(lineCharIdx, caretLineIdx)
            ?: Vector2f(0f, 0f)
        this.caretX = caretPos.x().roundToInt()
        this.caretY = caretPos.y().roundToInt()
        if (caret.posChanged) {
            this.requestCaretVisible(context)
            caret.posChanged = false
        }
    }
    
    private fun renderSelection() {
        val content: Text = this.content ?: return
        val selection: IntRange = this.selection ?: return
        var offset = 0
        var lineIdx = 0
        while (offset < this.actualValue.size) {
            val remaining: Int = this.actualValue.size - offset
            var lineLen: Int = this.actualValue
                .subList(offset, this.actualValue.size)
                .indexOf(EOL)
            if (lineLen == -1) { lineLen = remaining }
            val absSelectStart: Int = maxOf(selection.first, offset)
            val absSelectEnd: Int = minOf(selection.last, offset + lineLen)
            if (absSelectStart <= absSelectEnd) {
                val start: Vector2f = content
                    .posOfCharIdx(absSelectStart - offset, lineIdx)
                    ?: Vector2f(0f, 0f)
                val end: Vector2f = content
                    .posOfCharIdx(absSelectEnd - offset, lineIdx)
                    ?: Vector2f(0f, 0f)
                val width: Float = end.x() - start.x()
                fillColor(
                    TextInput.selectColor, this.target,
                    start, Vector2f(width, content.lineHeightPx)
                )
            }
            offset += lineLen + 1
            lineIdx += 1
        }
    }
    
    override fun render(context: UiElementContext) {
        this.computeCaretPosition(context)
        this.prepareTarget()
        val placeholder: Text? = this.placeholder
        if (placeholder != null && this.value.isEmpty()) {
            blitTexture(
                placeholder.result, this.target,
                0, 0, placeholder.pxWidth, placeholder.pxHeight
            )
        }
        val content: Text? = this.content
        if (content != null) {
            this.renderSelection()
            blitTexture(
                content.result, this.target,
                0, 0, content.pxWidth, content.pxHeight
            )
            val caret: Caret? = this.caret
            if (caret != null && caret.blinkIsVisible) {
                fillColor(
                    TextInput.caretColor, this.target,
                    this.caretX, this.caretY,
                    TextInput.CARET_WIDTH,
                    content.lineHeightPx.roundToInt()
                )
            }
        }
    }
    
    fun withContent(content: Text?): TextInput {
        this.content = content
        return this
    }
    
    fun withoutContent(): TextInput = this.withContent(null)

    fun withValue(value: String): TextInput {
        this.actualValue.clear()
        this.actualValue = value.codePoints().asSequence().toMutableList()
        this.updateDisplayedValue()
        this.invalidate()
        return this
    }
    
    fun withPlaceholder(placeholder: Text?): TextInput {
        this.placeholder = placeholder
        return this
    }
    
    fun withoutPlaceholder(): TextInput = this.withPlaceholder(null)
    
    fun withDisplayedSpans(f: (String) -> List<Span>): TextInput {
        this.displayedValue = { codepoints: List<Int> ->
            val str: String = codepoints
                .joinToString("", transform = Character::toString)
            f(str)
        }
        return this
    }
        
    fun withDisplayedText(f: (String) -> String): TextInput
        = this.withDisplayedSpans { text: String ->
            listOf(Span(f(text)))
        }
        
    fun withTypedCodepoints(f: (Int) -> Unit): TextInput {
        this.onTypedText = f
        return this
    }
    
    fun withTypedText(f: (String) -> Unit): TextInput
        = this.withTypedCodepoints { cp -> f(Character.toString(cp)) }
     
    fun withDeletedCodepoints(f: (Int) -> Unit): TextInput {
        this.onDeletedText = f
        return this
    }
    
    fun withDeletedText(f: (String) -> Unit): TextInput
        = this.withDeletedCodepoints { cp -> f(Character.toString(cp)) }
        
    fun withMultilineInput(multiline: Boolean = true): TextInput {
        this.isMultiline = multiline
        return this
    }
    
}

data class CodeEditingSettings(
    val paired: List<String>,
    val autoIndent: Boolean
)

fun TextInput.withCodeTypedHandler(
    settings: CodeEditingSettings
): TextInput = this.withTypedCodepoints { cp ->
    val (paired, autoIndent) = settings
    val caret: TextInput.Caret = this.caret ?: return@withTypedCodepoints
    val c: String = Character.toString(cp)
    if (paired.any { it.substring(1, 2) == c }) {
        if (this.value.getOrNull(caret.offset) == cp) {
            this.caret?.moveRight(this.value)
            return@withTypedCodepoints
        }
    }
    val pair: String? = paired.find { it.substring(0, 1) == c }
    if (pair != null) {
        this.writeText(pair)
        this.caret?.moveLeft(this.value)
        return@withTypedCodepoints
    }
    if (autoIndent && c == "\n") {
        var lineStart: Int = this.value
            .subList(0, caret.offset)
            .lastIndexOf(EOL)
        if (lineStart == -1) { lineStart = 0 }
        else { lineStart += 1 }
        var indentLevel: Int = this.value
            .subList(lineStart, this.value.size)
            .indexOfFirst { it != SPACE }
        if (indentLevel == -1) { indentLevel = this.value.size - lineStart }
        val beforeCp: Int? = this.value.getOrNull(caret.offset - 1)
        val beforeIsPair: Boolean = beforeCp != null
            && paired.any { it.substring(0, 1) == Character.toString(beforeCp) }
        val afterCp: Int? = this.value.getOrNull(caret.offset)
        val afterIsPair: Boolean = afterCp != null
            && paired.any { it.substring(1, 2) == Character.toString(afterCp) }
        val innerIndentLevel: Int = indentLevel + if (!beforeIsPair) { 0 }
            else { this.tabLength }
        this.writeText(c)
        this.writeText(" ".repeat(innerIndentLevel))
        if (beforeIsPair && afterIsPair) {
            this.writeText(EOL)
            this.writeText(" ".repeat(indentLevel))
            for (i in 1..indentLevel + 1) {
                caret.moveLeft(this.value)
            }
        }
        return@withTypedCodepoints
    }
    this.writeText(c)
}

fun TextInput.withCodeDeletedHandler(
    settings: CodeEditingSettings
): TextInput = this.withDeletedCodepoints { cp ->
    val (paired, autoIndent) = settings
    val caret: TextInput.Caret = this.caret ?: return@withDeletedCodepoints
    val c: String = Character.toString(cp)
    val afterCp: Int? = this.value.getOrNull(caret.offset)
    val aroundCaret: String = c +
        if (afterCp == null) { null } else { Character.toString(afterCp) }
    if (paired.any { it == aroundCaret }) {
        caret.moveRight(this.value)
        this.deleteLeft(2)
        return@withDeletedCodepoints
    }
    var lineStart = this.value
        .subList(0, caret.offset)
        .lastIndexOf(EOL)
    if (lineStart == -1) { lineStart = 0 }
    else { lineStart += 1 }
    val caretLineIdx: Int = caret.offset - lineStart
    var prevTabDist: Int = caretLineIdx % this.tabLength
    if (prevTabDist == 0 && caretLineIdx >= 1) {
        prevTabDist = minOf(caretLineIdx, this.tabLength)
    }
    val untilTabSpaces: Boolean = this.value
        .subList(lineStart + caretLineIdx - prevTabDist, caret.offset)
        .all { it == SPACE }
    if (untilTabSpaces && prevTabDist != 0) {
        this.deleteLeft(prevTabDist)
        return@withDeletedCodepoints
    }
    this.deleteLeft(1)
}