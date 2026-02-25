
package schwalbe.ventura.server.game

import schwalbe.ventura.data.Item
import schwalbe.ventura.data.ItemType
import schwalbe.ventura.data.ItemVariant
import schwalbe.ventura.data.RobotStatus
import schwalbe.ventura.data.RobotType
import schwalbe.ventura.net.SerVector3

enum class EnemyRobotConfig(
    val robotType: RobotType,
    val baseItem: Item,
    val weaponItem: Item?
) {
    BASIC(
        robotType = RobotType.SCOUT,
        baseItem = Item(
            ItemType.KENDAL_DYNAMICS_SCOUT,
            ItemVariant.SCOUT_CAMOUFLAGE
        ),
        weaponItem = Item(ItemType.LASER)
    )
}

class EnemyRobot(
    val config: EnemyRobotConfig,
    override var position: SerVector3
) : Robot() {

    override val type: RobotType
        get() = this.config.robotType
    override val name: String
        get() = "Rogue Robot"
    override val status: RobotStatus
        get() = RobotStatus.RUNNING
    override var health: Float = this.type.maxHealth

    override val baseItem: Item
        get() = this.config.baseItem
    override val weaponItem: Item?
        get() = this.config.weaponItem

    init {
        this.alignPosition()
    }

    fun update(world: World) {
        super.update()
    }

}
