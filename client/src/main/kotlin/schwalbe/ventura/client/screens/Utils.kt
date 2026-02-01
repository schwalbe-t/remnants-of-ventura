
package schwalbe.ventura.client.screens

import org.joml.Vector4fc
import org.joml.Vector4f
import schwalbe.ventura.engine.ui.*

val BASE_FONT_COLOR: Vector4fc              = Vector4f(15f, 15f, 15f, 255f).div(255f)
val SECONDARY_FONT_COLOR: Vector4fc         = Vector4f(128f, 128f, 128f, 255f).div(255f)
val BRIGHT_FONT_COLOR: Vector4fc            = Vector4f(0.9f, 0.9f, 0.9f, 1f)
val SECONDARY_BRIGHT_FONT_COLOR: Vector4fc  = Vector4f(0.75f, 0.75f, 0.75f, 1f)

val PANEL_BACKGROUND: Vector4fc             = Vector4f(0f, 0f, 0f, 10f).div(255f)

val BUTTON_COLOR: Vector4fc                 = Vector4f(25f, 25f, 25f, 25f).div(255f)
val BUTTON_HOVER_COLOR: Vector4fc           = Vector4f(255f, 255f, 255f, 128f).div(255f)
val INPUT_COLOR: Vector4fc = BUTTON_COLOR

fun createTextInput(
    input: TextInput, placeholder: String, value: String,
    font: Font? = null,
    disp: (String) -> String = { it },
    maxLength: Int = Int.MAX_VALUE
): UiElement = Stack()
    .add(BlurBackground().withRadius(5))
    .add(FlatBackground().withColor(INPUT_COLOR))
    .add(input
        .withContent(Text()
            .withFont(font)
            .withSize(70.ph)
        )
        .withValue(value)
        .withPlaceholder(Text()
            .withSize(70.ph)
            .withColor(SECONDARY_FONT_COLOR)
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
    .wrapBorderRadius(0.75.vmin)

fun addLabelledInput(
    axis: Axis, name: String, placeholder: String, value: String,
    font: Font,
    disp: (String) -> String = { it },
    maxLength: Int = Int.MAX_VALUE
): TextInput {
    val input = TextInput()
    axis.add(3.vmin, Text()
        .withText(name)
        .withSize(2.vmin)
        .pad(bottom = 0.5.vmin, left = 1.vmin)
    )
    axis.add(7.vmin,
        createTextInput(
            input, placeholder, value, font, disp, maxLength
        ).pad(bottom = 2.vmin)
    )
    return input
}

fun createTextButton(
    content: String,
    handler: () -> Unit,
    textColor: Vector4fc = BASE_FONT_COLOR
): UiElement = createButton(
    content = Text()
        .withText(content)
        .withColor(textColor)
        .withSize(70.ph)
        .alignCenter()
        .pad(0.75.vmin),
    handler
)

fun createButton(
    content: UiElement,
    handler: () -> Unit
): UiElement = Stack()
    .add(BlurBackground()
        .withRadius(5)
    )
    .add(FlatBackground()
        .withColor(BUTTON_COLOR)
        .withHoverColor(BUTTON_HOVER_COLOR)
    )
    .add(content)
    .add(ClickArea().withHandler(handler))
    .wrapBorderRadius(0.75.vmin)
