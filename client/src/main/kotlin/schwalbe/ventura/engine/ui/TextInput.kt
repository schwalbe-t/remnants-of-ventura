
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.input.*
import org.joml.*
import kotlin.math.roundToInt

class TextInput : GpuUiElement(), Focusable {
    
    companion object {
        val cursorColor: Vector4fc = Vector4f(90f, 193f, 254f, 255f).div(255f)
        const val CURSOR_WIDTH = 2
        val rightCursorPadding: UiSize = 5.vmin
        var cursorScrollPadding: UiSize = 10.vmin
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
        
    var displayedValue: (List<Int>) -> String
        = { it.joinToString("", transform = Character::toString) }
        set(value) {
            field = value
            this.invalidate()
        }
        
    private var actualValue: MutableList<Int> = mutableListOf()
    private var actualValueStr: String = ""
    var value: List<Int>
        get() = this.actualValue
        set(value) {
            this.actualValue = value.toMutableList()
            this.updateDisplayedValue()
        }
    
    override val children: List<UiElement>
        get() = listOfNotNull(this.placeholder, this.content)
    
    private var cursorPosChanged: Boolean = false
    private var cursor: Int? = null
        set(value) {
            if (value != field) {
                this.cursorPosChanged = true
                this.invalidate()
            }
            field = value
        }
    private var selection: IntRange? = null
    
    override fun onFocusLost() {
        this.cursor = null
        this.invalidate()
    }
    
    private fun charIndexOf(cursorPos: Int): Int
        = this.actualValueStr.offsetByCodePoints(0, cursorPos)
    
    private fun cursorPosOf(charIndex: Int): Int
        = this.actualValueStr.codePointCount(0, charIndex)
    
    private fun updateDisplayedValue() {
        this.actualValueStr = this.actualValue
            .joinToString("", transform = Character::toString)
        this.content?.withText(this.displayedValue(this.actualValue))
        this.invalidate()
    }
    
    private fun clearSelection() {
        val cursorPos: Int = this.cursor ?: return
        val selection: IntRange = this.selection ?: return
        this.actualValue.subList(selection.first, selection.last + 1).clear()
        this.selection = null
    }
    
    override fun captureInput(context: UiElementContext) {
        val mouseInside: Boolean = Mouse.isInsideArea(
            context.visibleAbsLeft, context.visibleAbsTop,
            context.visibleAbsRight, context.visibleAbsBottom
        )
        val mouseLeftPressed: Boolean = context.global.input
            .remainingOfType<MButtonDown>()
            .any { it.button == MButton.LEFT }
        if (this.cursor == null && mouseInside && mouseLeftPressed) {
            context.global.currentlyInFocus = this
            this.cursor = this.value.size
            this.selection = null
            this.invalidate()
        }
        if (this.cursor != null && !mouseInside && mouseLeftPressed) {
            context.global.currentlyInFocus = null
        }
        var cursorPos: Int? = this.cursor
        if (cursorPos != null) {
            for (e in context.global.input.remaining()) {
                when (e) {
                    is CharTyped -> {
                        this.clearSelection()
                        this.actualValue.add(cursorPos, e.codepoint)
                        this.cursor = (this.cursor ?: 0) + 1
                        this.updateDisplayedValue()
                    }
                    is KeyDown, is KeyRepeat -> when (e.key) {
                        Key.ENTER -> {
                            if (!this.isMultiline) { continue }
                            this.clearSelection()
                            this.actualValue.add(cursorPos, 0x000A) // '\n'
                            this.cursor = (this.cursor ?: 0) + 1
                            this.updateDisplayedValue()
                        }
                        Key.BACKSPACE -> {
                            if (cursorPos >= 1) {
                                this.actualValue.removeAt(cursorPos - 1)
                                this.cursor = (this.cursor ?: 0) - 1
                                this.updateDisplayedValue()
                            }
                        }
                        Key.LEFT -> {
                            this.cursor = maxOf((this.cursor ?: 0) - 1, 0)
                        }
                        Key.RIGHT -> {
                            this.cursor = minOf(
                                (this.cursor ?: 0) + 1,
                                this.value.size
                            )
                        }
                        else -> continue
                    }
                    else -> continue
                }
                context.global.input.remove(e)
            }
        }
    }
    
    private fun requestCursorVisible(context: UiElementContext) {
        val scroll: Scroll = context.parent as? Scroll ?: return
        val padding: Int = TextInput.cursorScrollPadding(context).roundToInt()
        scroll.requestVisible(this.cursorX, this.cursorY, padding)
    }
    
    override fun updateLayout(context: UiElementContext) {
        val content: Text? = this.content
        if (content != null) {
            content.withWrapping(false)
            content.withSize(0.px, 0.px)
            val cursorPadding: Int = TextInput
                .rightCursorPadding(context).roundToInt()
            this.pxWidth = maxOf(this.pxWidth, content.pxWidth + cursorPadding)
            this.pxHeight = maxOf(
                this.pxHeight,
                content.pxHeight + if (!this.isMultiline) { 0 }
                    else { context.parentPxHeight / 2 }
            )
        }
    }
    
    private var cursorX: Int = 0
    private var cursorY: Int = 0
    
    private fun computeCursorPosition(context: UiElementContext) {
        val cursorPos: Int = this.cursor ?: return
        val content: Text = this.content ?: return
        val cursorCharIdx: Int = this.charIndexOf(cursorPos)
        val beforeCursor: String = this.actualValueStr
            .substring(0, cursorCharIdx)
        val cursorLineIdx: Int = beforeCursor.count { it == '\n' }
        val lineCharIdx: Int = cursorCharIdx -
            beforeCursor.lastIndexOf('\n') - 1
        val cursor: Vector2f = content
            .posOfCharIdx(lineCharIdx, cursorLineIdx)
            ?: Vector2f(0f, 0f)
        this.cursorX = cursor.x().roundToInt()
        this.cursorY = cursor.y().roundToInt()
        if (this.cursorPosChanged) {
            this.requestCursorVisible(context)
            this.cursorPosChanged = false
        }
    }
    
    override fun render(context: UiElementContext) {
        this.computeCursorPosition(context)
        this.prepareTarget()
        val placeholder: Text? = this.placeholder
        if (placeholder != null) {
            blitTexture(
                placeholder.result, this.target,
                0, 0, placeholder.pxWidth, placeholder.pxHeight
            )
        }
        val content: Text? = this.content
        if (content != null) {
            blitTexture(
                content.result, this.target,
                0, 0, content.pxWidth, content.pxHeight
            )
            val cursorPos: Int? = this.cursor
            if (cursorPos != null) {
                val fontSize: UiSize = content.fontSize
                    ?: context.global.defaultFontSize
                fillColor(
                    TextInput.cursorColor, this.target,
                    this.cursorX, this.cursorY,
                    TextInput.CURSOR_WIDTH,
                    content.lineHeightPx().roundToInt()
                )
            }
        }
    }
    
    fun withContent(content: Text?): TextInput {
        this.content = content
        return this
    }
    
    fun withoutContent(): TextInput = this.withContent(null)
    
    fun withPlaceholder(placeholder: Text?): TextInput {
        this.placeholder = placeholder
        return this
    }
    
    fun withoutPlaceholder(): TextInput = this.withPlaceholder(null)

    fun withDisplayedValue(f: (List<Int>) -> String): TextInput {
        this.displayedValue = f
        return this
    }
    
    fun withMultilineInput(multiline: Boolean = true): TextInput {
        this.isMultiline = multiline
        return this
    }
    
}