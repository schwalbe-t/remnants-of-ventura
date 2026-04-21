
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.ui.*
import org.joml.Vector4f
import org.joml.Vector4fc

object Theme {

    val FONT_COLOR: Vector4fc = Vector4f(0.9f, 0.9f, 0.9f, 1f)
    val SECONDARY_FONT_COLOR: Vector4fc = Vector4f(0.75f, 0.75f, 0.75f, 1f)

    val PANEL_BACKGROUND: Vector4fc = Vector4f(0.2f, 0.2f, 0.2f, 0.2f)

    val BUTTON_COLOR: Vector4fc = Vector4f(25f, 25f, 25f, 50f).div(255f)
    val BUTTON_HOVER_COLOR: Vector4fc = Vector4f(255f, 255f, 255f, 128f).div(255f)
    val INPUT_COLOR: Vector4fc = BUTTON_COLOR

    val TITLE_BAR_HEIGHT: UiSize = 5.vmin

    fun textInput(
        input: TextInput,
        placeholder: String,
        value: String,
        valColor: Vector4fc? = null,
        valFont: Font? = null,
        valSize: UiSize? = 70.ph,
        phColor: Vector4fc? = SECONDARY_FONT_COLOR,
        phFont: Font? = null,
        phSize: UiSize? = 70.ph,
        borderRadius: UiSize = 0.75.vmin,
        disp: (String) -> String = { it },
        maxLength: Int = Int.MAX_VALUE
    ): UiElement = Stack()
        .add(BlurBackground()
            .withRadius(5)
        )
        .add(FlatBackground()
            .withColor(INPUT_COLOR)
        )
        .add(input
            .withContent(Text()
                .withFont(valFont)
                .withSize(valSize)
                .withColor(valColor)
            )
            .withValue(value)
            .withPlaceholder(Text()
                .withFont(phFont)
                .withSize(phSize)
                .withColor(phColor)
                .withText(placeholder)
            )
            .withDisplayedText(disp)
            .withTypedText {
                if (input.value.size >= maxLength) { return@withTypedText }
                input.writeText(it)
            }
            .wrapScrolling()
            .withBarsEnabled(horiz = true, vert = false)
            .pad(1.25.vmin)
        )
        .wrapBorderRadius(borderRadius)

    fun addLabelledInput(
        onto: Axis,
        name: String,
        placeholder: String,
        value: String,
        labColor: Vector4fc? = null,
        labFont: Font? = null,
        valColor: Vector4fc? = null,
        valFont: Font? = null,
        valSize: UiSize? = 70.ph,
        phColor: Vector4fc? = SECONDARY_FONT_COLOR,
        phFont: Font? = null,
        phSize: UiSize? = 70.ph,
        borderRadius: UiSize = 0.75.vmin,
        disp: (String) -> String = { it },
        maxLength: Int = Int.MAX_VALUE
    ): TextInput {
        val input = TextInput()
        onto.add(3.vmin, Text()
            .withFont(labFont)
            .withColor(labColor)
            .withText(name)
            .withSize(2.vmin)
            .pad(bottom = 0.5.vmin, left = 1.vmin)
        )
        onto.add(7.vmin, textInput(
            input, placeholder, value, valColor, valFont, valSize, phColor,
            phFont, phSize, borderRadius, disp, maxLength
        ).pad(bottom = 2.vmin))
        return input
    }

    fun button(
        content: UiElement,
        handler: () -> Unit,
        borderRadius: UiSize = 0.75.vmin
    ): UiElement = Stack()
        .add(BlurBackground()
            .withRadius(5)
        )
        .add(FlatBackground()
            .withColor(BUTTON_COLOR)
            .withHoverColor(BUTTON_HOVER_COLOR)
        )
        .add(content)
        .add(ClickArea()
            .withLeftHandler(handler)
        )
        .wrapBorderRadius(borderRadius)

    fun button(
        content: String,
        handler: () -> Unit,
        color: Vector4fc? = null,
        font: Font? = null,
        size: UiSize? = 70.ph,
        alignment: Text.Alignment = Text.Alignment.CENTER,
        padding: UiSize = 0.75.vmin,
        borderRadius: UiSize = 0.75.vmin
    ): UiElement = button(
        content = Text()
            .withText(content)
            .withColor(color)
            .withFont(font)
            .withSize(size)
            .withAlignment(alignment)
            .pad(padding),
        handler,
        borderRadius
    )

}

fun UiElement.wrapThemedScrolling(
    horiz: Boolean = true, vert: Boolean = true
): Scroll = this
    .wrapScrolling(horiz, vert)
    .withThumbColor(Theme.BUTTON_COLOR)
    .withThumbHoverColor(Theme.BUTTON_HOVER_COLOR)

fun UiElement.withTitlebar(text: String) = Axis.column()
    .add(Theme.TITLE_BAR_HEIGHT, Stack()
        .add(BlurBackground().withRadius(5))
        .add(FlatBackground().withColor(Theme.BUTTON_COLOR))
        .add(Text()
            .withText(text)
            .withSize(75.ph)
            .withFont(googleSansSb())
            .pad(top = 1.25.vmin, bottom = 1.25.vmin, left = 2.vmin)
        )
    )
    .add(100.ph - Theme.TITLE_BAR_HEIGHT, this)
