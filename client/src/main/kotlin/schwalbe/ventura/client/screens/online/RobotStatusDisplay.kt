
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.game.WorldState
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.data.RobotStatus
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.net.*
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.uuid.Uuid

class RobotStatusDisplay {

    companion object {
        fun createStatusProp(nameText: Text, valueText: Text) = Axis.row()
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
            .pad(vertical = 0.1.vmin, horizontal = 0.px)

        fun updateDisplayTexts(
            health: Text, memory: Text, processor: Text, pi: PrivateRobotInfo
        ) {
            val pHealth: Int = (pi.fracHealth * 100f).roundToInt()
            val pMemUsage: Int = (pi.fracMemUsage * 100f).roundToInt()
            val pCpuUsage: Int = (pi.fracCpuUsage * 100f).roundToInt()
            health.withText("$pHealth%")
            health.withColor(when {
                pHealth >= 70 -> RobotStatus.StatusColor.GREEN
                pHealth >= 10 -> RobotStatus.StatusColor.YELLOW
                else -> RobotStatus.StatusColor.RED
            })
            memory.withText("$pMemUsage%")
            memory.withColor(when {
                pMemUsage <= 70 -> RobotStatus.StatusColor.GREEN
                pMemUsage <= 90 -> RobotStatus.StatusColor.RED
                else -> RobotStatus.StatusColor.RED
            })
            processor.withText("$pCpuUsage%")
            processor.withColor(when {
                pCpuUsage <= 70 -> RobotStatus.StatusColor.GREEN
                pCpuUsage <= 90 -> RobotStatus.StatusColor.YELLOW
                else -> RobotStatus.StatusColor.RED
            })
        }
    }


    val nameText: Text = Text()
    val statusText: Text = Text()
    val hpValueText: Text = Text()
    val ramValueText: Text = Text()
    val cpuValueText: Text = Text()
    val toggleButtonText: Text = Text()
    val toggleButtonClick: ClickArea = ClickArea()
    val robotDisplay: Stack = Stack()

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
                .add(RobotStatusDisplay.createStatusProp(
                    Text().withText(localized()[LABEL_ROBOT_STAT_HEALTH]),
                    this.hpValueText
                ))
                .add(RobotStatusDisplay.createStatusProp(
                    Text().withText(localized()[LABEL_ROBOT_STAT_MEMORY]),
                    this.ramValueText
                ))
                .add(RobotStatusDisplay.createStatusProp(
                    Text().withText(localized()[LABEL_ROBOT_STAT_PROCESSOR]),
                    this.cpuValueText
                ))
            )
            .add(1.5.vmin, Space())
            .add(100.ph, this.robotDisplay)
        )

    val root = Stack()
        .add(BlurBackground()
            .withRadius(2)
            .withSpread(4)
        )
        .add(FlatBackground().withColor(PANEL_BACKGROUND))
        .add(this.content.pad(1.5.vmin))
        .wrapBorderRadius(0.75.vmin)
        .pad()

    fun update(
        id: Uuid, si: SharedRobotInfo, pi: PrivateRobotInfo, client: Client
    ) {
        val l = localized()
        this.nameText.withText(si.name)
        this.statusText.withText(l[si.status.localNameKey])
        this.statusText.withColor(si.status.displayColor)
        RobotStatusDisplay.updateDisplayTexts(
            this.hpValueText, this.ramValueText, this.cpuValueText, pi
        )
        val toggleButtonLabel: LocalKeys =
            if (!si.status.isRunning) { BUTTON_ROBOT_START }
            else { BUTTON_ROBOT_STOP }
        this.toggleButtonText.withText(l[toggleButtonLabel])
        this.toggleButtonClick.withHandler {
            val action: PacketType<Uuid> =
                if (!si.status.isRunning) { PacketType.START_ROBOT }
                else { PacketType.STOP_ROBOT }
            client.network.outPackets?.send(Packet.serialize(action, id))
        }
        if (this.robotDisplay.children.isEmpty()) {
            this.robotDisplay.add(ItemDisplay.createDisplay(
                item = si.item,
                fixedAngle = -(PI.toFloat() / 4f), // 180/4 = 45 degrees
                msaaSamples = 4
            ))
        }
    }

}


class RobotStatusDisplayManager {
    val entries: MutableMap<Uuid, RobotStatusDisplay> = mutableMapOf()
    val entryRoots: Axis = Axis.row()
    val container: Padding = this.entryRoots
        .wrapScrolling(vert = true, horiz = true)
        .withThumbColor(BUTTON_COLOR)
        .withThumbHoverColor(BUTTON_HOVER_COLOR)
        .withScrollInputFunc { dx, dy -> Pair(dx + dy, 0f) }
        .pad()
}

fun RobotStatusDisplayManager.update(
    client: Client, worldState: WorldStatePacket
) {
    val p: UiSize = 1.5.vmin
    val entryWidth: UiSize = 30.vmin + p
    val entryHeight: UiSize = 15.vmin
    val centeringPad: UiSize = maxOf(
        (100.vw - (entryWidth * worldState.ownedRobots.size) - p) / 2, 0.px
    )
    this.entries
        .filter { (id, _) -> id !in worldState.ownedRobots.keys }
        .forEach { (id, disp) ->
            this.entries.remove(id)
            this.entryRoots.dispose(disp.root)
        }
    for ((id, privateInfo) in worldState.ownedRobots) {
        val sharedInfo: SharedRobotInfo = worldState.allRobots[id] ?: continue
        val disp: RobotStatusDisplay = this.entries.getOrPut(id) {
            val d = RobotStatusDisplay()
            this.entryRoots.add(entryWidth, d.root.withPadding(
                left = p, right = 0.px, top = 0.px, bottom = 0.px
            ))
            d
        }
        disp.update(id, sharedInfo, privateInfo, client)
    }
    this.container.withPadding(
        left = floor(centeringPad), right = floor(centeringPad) + p,
        top = 100.vh - entryHeight - p,
        bottom = 0.px
    )
}
