
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.screens.*
import schwalbe.ventura.engine.ui.*
import org.joml.Vector4fc
import org.joml.Vector4f

private val STATUS_RUNNING_COLOR: Vector4fc = Vector4f(0.35f, 0.85f, 0.45f, 1f)
private val STATUS_ERROR_COLOR: Vector4fc = Vector4f(1.0f, 0.45f, 0.55f, 1f)
private val STATUS_STOPPED_COLOR: Vector4fc = Vector4f(0.7f, 0.7f, 0.75f, 1f)

object RobotStatusDisplay {

    fun createDisplay(): UiElement = Stack()
        .add(BlurBackground()
            .withRadius(3)
            .withSpread(3)
        )
        .add(FlatBackground().withColor(PANEL_BACKGROUND))
        .add(Stack().pad(1.5.vmin))
        .wrapBorderRadius(0.75.vmin)

    private fun createStatusProp(nameText: String, valueText: String)
        = Axis.row()
        .add(100.pw - 5.vmin, Text()
            .withText(nameText)
            .withSize(80.ph)
            .withColor(BRIGHT_FONT_COLOR)
        )
        .add(5.vmin, Text()
            .withText(valueText)
            .withSize(75.ph)
            .withColor(BRIGHT_FONT_COLOR)
            .withFont(jetbrainsMonoSb())
            .alignRight()
        )

    fun updateDisplay(root: UiElement) {
        val contentContainer: Stack = root.children.getOrNull(0)
            ?.children?.getOrNull(2)
            ?.children?.getOrNull(0)
            as? Stack ?: return
        contentContainer.disposeAll()
        contentContainer.add(Axis.column()
            .add(3.vmin, Text()
                .withText("Unnamed Robot")
                .withSize(75.ph)
                .withFont(googleSansSb())
                .withColor(BRIGHT_FONT_COLOR)
                .withWrapping(enabled = false)
            )
            .add(2.5.vmin, Text()
                .withText(when (root.toString().hashCode() % 3) {
                    0 -> "Running"
                    1 -> "Encountered Error"
                    else -> "Stopped"
                })
                .withSize(60.ph)
                .withColor(when (root.toString().hashCode() % 3) {
                    0 -> STATUS_RUNNING_COLOR
                    1 -> STATUS_ERROR_COLOR
                    else -> STATUS_STOPPED_COLOR
                })
                .withFont(googleSansSb())
            )
            .add(100.ph - 2.5.vmin - 3.vmin, Axis.row()
                .add(100.pw - 1.5.vmin - 100.ph, Axis.column()
                    .add(100.ph / 3, createStatusProp("Health:", "33%"))
                    .add(100.ph / 3, createStatusProp("Memory:", "100%"))
                    .add(100.ph / 3, createStatusProp("Processor:", "44%"))
                    .pad(vertical = 1.vmin, horizontal = 0.px)
                )
                .add(1.5.vmin, Space())
                // TODO! 3D render of robot instead of blue rectangle
                .add(100.ph, FlatBackground().withColor(0, 0, 255))
            )
        )
    }

}


class RobotStatusDisplayManager {
    val entryList: Axis = Axis.row()
    val container: Padding = this.entryList
        .wrapScrolling(vert = true, horiz = true)
        .withThumbColor(BUTTON_COLOR)
        .withThumbHoverColor(BUTTON_HOVER_COLOR)
        .pad()
}

fun RobotStatusDisplayManager.update(numEntries: Int) {
    val p: UiSize = 1.5.vmin
    val entryWidth: UiSize = 30.vmin + p
    val entryHeight: UiSize = 15.vmin
    val centeringPad: UiSize = maxOf(
        (100.vw - (entryWidth * numEntries) - p) / 2, 0.px
    )
    val numExistingEntries: Int = this.entryList.children.size
    if (numEntries < numExistingEntries) {
        this.entryList.children
            .subList(numEntries, numExistingEntries)
            .toList().forEach(this.entryList::dispose)
    }
    if (numEntries > numExistingEntries) {
        for (i in numExistingEntries..<numEntries) {
            val d = RobotStatusDisplay.createDisplay().pad(left = p)
            this.entryList.add(entryWidth, d)
        }
    }
    for (padded in this.entryList.children) {
        val root = padded.children[0]
        RobotStatusDisplay.updateDisplay(root)
    }
    this.container.withPadding(
        left = floor(centeringPad), right = floor(centeringPad) + p,
        top = 100.vh - entryHeight - p,
        bottom = 0.px
    )
}
