
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.screens.BUTTON_COLOR
import schwalbe.ventura.client.screens.BUTTON_HOVER_COLOR
import schwalbe.ventura.client.screens.PANEL_BACKGROUND
import schwalbe.ventura.engine.ui.*

object RobotStatusDisplay {

    fun createDisplay(): UiElement = Stack()
        .add(BlurBackground()
            .withRadius(3)
            .withSpread(3)
        )
        .add(FlatBackground()
            .withColor(PANEL_BACKGROUND)
        )
        .wrapBorderRadius(0.75.vmin)

    fun updateDisplay(root: UiElement) {

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

fun RobotStatusDisplayManager.update() {
    val numEntries = 10
    val p: UiSize = 1.5.vmin
    val entryWidth: UiSize = 30.vmin + p
    val entryHeight: UiSize = 15.vmin
    val centeringPad: UiSize = maxOf(
        (100.vw - (entryWidth * numEntries) - p) / 2, 0.px
    )
    this.entryList.disposeAll()
    for (i in 1..numEntries) {
        var padded: UiElement? = this.entryList.children.getOrNull(i)
        if (padded == null) {
            padded = RobotStatusDisplay.createDisplay().pad(left = p)
            this.entryList.add(entryWidth, padded)
        }
        val root = padded.children[0]
        RobotStatusDisplay.updateDisplay(root)
    }
    this.container.withPadding(
        left = floor(centeringPad), right = floor(centeringPad) + p,
        top = 100.vh - entryHeight - p,
        bottom = 0.px
    )
}
