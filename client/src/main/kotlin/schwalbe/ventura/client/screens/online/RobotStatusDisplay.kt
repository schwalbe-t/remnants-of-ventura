
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.screens.*
import schwalbe.ventura.engine.ui.*
import org.joml.Vector4fc
import org.joml.Vector4f

private val STATUS_RUNNING_COLOR: Vector4fc = Vector4f(0.35f, 0.85f, 0.45f, 1f)
private val STATUS_ERROR_COLOR: Vector4fc = Vector4f(1.0f, 0.45f, 0.55f, 1f)
private val STATUS_STOPPED_COLOR: Vector4fc = Vector4f(0.7f, 0.7f, 0.75f, 1f)

class RobotStatusDisplay {

    private fun createStatusProp(nameText: Text, valueText: Text)
        = Axis.row()
        .add(100.pw - 5.vmin, nameText
            .withSize(80.ph)
            .withColor(BRIGHT_FONT_COLOR)
        )
        .add(5.vmin, valueText
            .withSize(75.ph)
            .withColor(BRIGHT_FONT_COLOR)
            .withFont(jetbrainsMonoSb())
            .alignRight()
        )
        .pad(0.1.vmin)

    val nameText: Text = Text()
    val statusText: Text = Text()
    val hpValueText: Text = Text()
    val ramValueText: Text = Text()
    val cpuValueText: Text = Text()
    val toggleButtonText: Text = Text()
    val toggleButtonClick: ClickArea = ClickArea()

    val content = Axis.column()
        .add(3.vmin, this.nameText
            .withSize(75.ph)
            .withFont(googleSansSb())
            .withColor(BRIGHT_FONT_COLOR)
            .withWrapping(enabled = false)
        )
        .add(2.5.vmin, this.statusText
            .withSize(60.ph)
            .withFont(googleSansSb())
        )
        .add(100.ph - 2.5.vmin - 3.vmin, Axis.row()
            .add(100.pw - 1.5.vmin - 100.ph, Axis.column(100.ph / 4)
                .add(Stack()
                    .add(FlatBackground()
                        .withColor(BUTTON_COLOR)
                        .withHoverColor(BUTTON_HOVER_COLOR)
                    )
                    .add(this.toggleButtonText
                        .withSize(75.ph)
                        .withColor(BRIGHT_FONT_COLOR)
                        .alignCenter()
                        .pad(0.1.vmin)
                    )
                    .add(this.toggleButtonClick)
                    .wrapBorderRadius(0.5.vmin)
                )
                .add(createStatusProp(
                    Text().withText("Health:"), this.hpValueText
                ))
                .add(createStatusProp(
                    Text().withText("Memory:"), this.ramValueText
                ))
                .add(createStatusProp(
                    Text().withText("Processor:"), this.cpuValueText
                ))
            )
            .add(1.5.vmin, Space())
            // TODO! 3D render of robot instead of blue rectangle
            .add(100.ph, FlatBackground().withColor(0, 0, 255))
        )

    val root = Stack()
        .add(BlurBackground()
            .withRadius(3)
            .withSpread(3)
        )
        .add(FlatBackground().withColor(PANEL_BACKGROUND))
        .add(this.content.pad(1.5.vmin))
        .wrapBorderRadius(0.75.vmin)

    fun update() {
        this.nameText.withText("Unnamed Robot")
        this.statusText.withText(when (this.toString().hashCode() % 3) {
            0 -> "Running"
            1 -> "Encountered Error"
            else -> "Stopped"
        })
        this.statusText.withColor(when (this.toString().hashCode() % 3) {
            0 -> STATUS_RUNNING_COLOR
            1 -> STATUS_ERROR_COLOR
            else -> STATUS_STOPPED_COLOR
        })
        this.hpValueText.withText("33%")
        this.ramValueText.withText("100%")
        this.cpuValueText.withText("42%")
        this.toggleButtonText.withText("Start")
        this.toggleButtonClick.withHandler {
            // TODO!
            println("Start/Stop Robot")
        }
    }

}


class RobotStatusDisplayManager {
    val entries: MutableList<RobotStatusDisplay> = mutableListOf()
    val entryRoots: Axis = Axis.row()
    val container: Padding = this.entryRoots
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
    val numExistingEntries: Int = this.entryRoots.children.size
    if (numEntries < numExistingEntries) {
        this.entries.subList(numEntries, numExistingEntries).clear()
        this.entryRoots.children
            .subList(numEntries, numExistingEntries)
            .toList().forEach(this.entryRoots::dispose)
    }
    if (numEntries > numExistingEntries) {
        for (i in numExistingEntries..<numEntries) {
            val d = RobotStatusDisplay()
            this.entries.add(d)
            this.entryRoots.add(entryWidth, d.root.pad(left = p))
        }
    }
    this.entries.forEach(RobotStatusDisplay::update)
    this.container.withPadding(
        left = floor(centeringPad), right = floor(centeringPad) + p,
        top = 100.vh - entryHeight - p,
        bottom = 0.px
    )
}
