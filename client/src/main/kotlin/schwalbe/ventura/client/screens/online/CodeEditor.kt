
package schwalbe.ventura.client.screens.online

import org.joml.Vector4f
import org.joml.Vector4fc
import schwalbe.ventura.client.screens.Theme
import schwalbe.ventura.client.screens.googleSansSb
import schwalbe.ventura.client.screens.jetbrainsMonoSb
import schwalbe.ventura.client.screens.wrapThemedScrolling
import schwalbe.ventura.engine.ui.Axis
import schwalbe.ventura.engine.ui.CodeEditingSettings
import schwalbe.ventura.engine.ui.Padding
import schwalbe.ventura.engine.ui.Scroll
import schwalbe.ventura.engine.ui.Space
import schwalbe.ventura.engine.ui.Text
import schwalbe.ventura.engine.ui.TextInput
import schwalbe.ventura.engine.ui.UiElement
import schwalbe.ventura.engine.ui.UiSize
import schwalbe.ventura.engine.ui.minus
import schwalbe.ventura.engine.ui.pad
import schwalbe.ventura.engine.ui.ph
import schwalbe.ventura.engine.ui.pw
import schwalbe.ventura.engine.ui.px
import schwalbe.ventura.engine.ui.vmin
import schwalbe.ventura.engine.ui.withCodeDeletedHandler
import schwalbe.ventura.engine.ui.withCodeTypedHandler
import schwalbe.ventura.engine.ui.withColor
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class CodeEditor(val file: File, fileContent: String, title: String) {

    companion object {
        val BACKGROUND_COLOR: Vector4fc
            = Vector4f(0.2f, 0.2f, 0.2f, 0.90f)

        val EDITING_SETTINGS = CodeEditingSettings(
            paired = listOf("()", "[]", "{}", "\"\""),
            autoIndent = true
        )
        const val SAVE_DELAY_MS: Long = 250
    }

    val lineNumbers: Text
    val lineNumberCont: Padding
    val textInput: TextInput
    val textScroll: Scroll
    val root: UiElement

    val path: Path = this.file.toPath()
    var lastEdit: Long? = null

    init {
        val fontSize: UiSize = 1.5.vmin
        this.lineNumbers = Text()
            .withFont(jetbrainsMonoSb())
            .withColor(Theme.SECONDARY_FONT_COLOR)
            .withSize(fontSize)
            .alignRight()
        this.lineNumberCont = Padding()
            .withPadding(0.px)
            .withContent(this.lineNumbers)
        this.textInput = TextInput()
            .withContent(Text()
                .withFont(jetbrainsMonoSb())
                .withSize(fontSize)
            )
            .withDisplayedSpans(::syntaxHighlightBigton)
            .withMultilineInput(multiline = true)
            .withValue(fileContent)
            .withCodeTypedHandler(EDITING_SETTINGS)
            .withCodeDeletedHandler(EDITING_SETTINGS)
            .withValueChangedHandler {
                this.updateLineNumbers()
                this.lastEdit = System.currentTimeMillis()
            }
        this.textScroll = this.textInput
            .wrapThemedScrolling(horiz = true, vert = true)
        this.root = Axis.column()
            .add(5.vmin, Text()
                .withText(title)
                .withFont(googleSansSb())
                .withSize(85.ph)
                .pad(1.5.vmin)
            )
            .add(100.ph - 5.vmin, Axis.row()
                .add(5.vmin, this.lineNumberCont)
                .add(3.vmin, Space())
                .add(100.pw - 5.vmin - 3.vmin, this.textScroll)
                .pad(0.5.vmin)
            )
        this.updateLineNumbers()
    }

    fun updateLineNumbers() {
        val lineNumberText = (1..this.textInput.computeLineCount())
            .joinToString(transform = Int::toString, separator = "\n")
        this.lineNumbers.withText(lineNumberText)
    }

    var lastScrollY: Float = 0f

    fun update() {
        val newScrollY: Float = -this.textScroll.scrollOffset.y()
        if (newScrollY != this.lastScrollY) {
            this.lineNumberCont.withPadding(
                top = newScrollY.px,
                bottom = 0.px, left = 0.px, right = 0.px
            )
            this.lineNumbers.invalidate()
            this.lastScrollY = newScrollY
        }
        val lastEdit: Long? = this.lastEdit
        val now: Long = System.currentTimeMillis()
        if (lastEdit != null && lastEdit + SAVE_DELAY_MS <= now) {
            this.save()
        }
    }

    fun save() {
        if (this.lastEdit == null) { return }
        Files.writeString(this.path, this.textInput.valueString)
        this.lastEdit = null
    }

}

fun CodeEditor.Companion.openFile(
    file: File, title: String = file.name
): CodeEditor? {
    try {
        val contents: String = Files.readString(file.toPath())
        return CodeEditor(file, contents, title)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
