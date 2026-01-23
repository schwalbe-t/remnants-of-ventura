
package schwalbe.ventura.client.screens

import org.joml.Vector4fc
import org.joml.Vector4f
import schwalbe.ventura.engine.ui.*

val BASE_FONT_COLOR: Vector4fc      = Vector4f(240f, 240f, 240f, 255f).div(255f)
val DARK_FONT_COLOR: Vector4fc      = Vector4f(128f, 128f, 128f, 255f).div(255f)
val BACKGROUND_COLOR: Vector4fc     = Vector4f(10f, 10f, 10f, 255f).div(255f)
val BUTTON_COLOR: Vector4fc         = Vector4f(25f, 25f, 25f, 255f).div(255f)
val BUTTON_HOVER_COLOR: Vector4fc   = Vector4f(50f, 50f, 50f, 255f).div(255f)
val INPUT_COLOR: Vector4fc = BUTTON_COLOR

fun createTextInput(
    input: TextInput, placeholder: String, value: String
): UiElement = Stack()
    .add(FlatBackground().withColor(INPUT_COLOR))
    .add(input
        .withContent(Text()
            .withSize(70.ph)
        )
        .withValue(value)
        .withPlaceholder(Text()
            .withSize(70.ph)
            .withColor(DARK_FONT_COLOR)
            .withText(placeholder)
        )
        .wrapScrolling()
        .withBarsEnabled(horiz = true, vert = false)
        .pad(1.25.vmin)
    )
    .wrapBorderRadius(1.vmin)

fun createTextButton(
    content: String,
    handler: () -> Unit
): UiElement = createButton(
    content = Text()
        .withText(content)
        .withSize(70.ph)
        .withAlignment(Text.Alignment.CENTER)
        .pad(0.5.vmin),
    handler
)

fun createButton(
    content: UiElement,
    handler: () -> Unit
): UiElement = Stack()
    .add(FlatBackground()
        .withColor(BUTTON_COLOR)
        .withHoverColor(BUTTON_HOVER_COLOR)
    )
    .add(content)
    .add(ClickArea().withHandler(handler))
    .wrapBorderRadius(1.vmin)