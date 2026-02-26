
package schwalbe.ventura.server.game

import schwalbe.ventura.data.*
import schwalbe.ventura.net.SerVector3
import schwalbe.ventura.utils.sign
import kotlin.math.abs

const val BASIC_ENEMY_SEARCH_RANGE: Int = 20
const val BASIC_ENEMY_MOVEMENT_SPEED: Int = 10 // ticks per unit moved
const val BASIC_ENEMY_TARGET_DIST: Int = 5
const val BASIC_ENEMY_ATTACK_RANGE: Int = 10
const val BASIC_ENEMY_ATTACK_DAMAGE: Float = 10f
const val BASIC_ENEMY_ATTACK_COOLDOWN: Long = 60

private fun updateBasicRobot(robot: EnemyRobot): (World) -> Unit {
    fun canShoot(toTgtX: Int, toTgtZ: Int, dist: Int) =
        (toTgtX == 0 || toTgtZ == 0) &&
        maxOf(abs(toTgtX), abs(toTgtZ)) <= dist
    fun moveTowards(rx: Int, rz: Int, toTgtX: Int, toTgtZ: Int, world: World) {
        if (robot.isMoving) { return }
        if (canShoot(toTgtX, toTgtZ, BASIC_ENEMY_TARGET_DIST)) { return }
        val collX: Boolean = world.data.chunkCollisions[rx + sign(toTgtX), rz]
        val collZ: Boolean = world.data.chunkCollisions[rx, rz + sign(toTgtZ)]
        val rMoveX: Int = if (collX) 0 else toTgtX
        val rMoveZ: Int = if (collZ) 0 else toTgtZ
        val (dx, dz) = if (rMoveZ == 0) { sign(rMoveX) to 0 }
            else { 0 to sign(rMoveZ) }
        robot.move(dx.toFloat(), dz.toFloat(), BASIC_ENEMY_MOVEMENT_SPEED)
    }
    var shootCooldown: Long = 0
    fun shoot(toTgtX: Int, toTgtZ: Int, target: PlayerRobot) {
        if (shootCooldown > 0) {
            shootCooldown -= 1
            return
        }
        if (!canShoot(toTgtX, toTgtZ, BASIC_ENEMY_ATTACK_RANGE)) { return }
        target.health -= BASIC_ENEMY_ATTACK_DAMAGE
        shootCooldown = BASIC_ENEMY_ATTACK_COOLDOWN
    }
    return update@{ world ->
        fun robotX(r: Robot) = r.position.x.unitsToUnitIdx()
        fun robotZ(r: Robot) = r.position.z.unitsToUnitIdx()
        val rx: Int = robotX(robot)
        val rz: Int = robotZ(robot)
        val target: PlayerRobot = world.players.values
            .flatMap { it.data.deployedRobots.values }
            .filter { world.data.peaceAreas.none { area ->
                area.contains(robotX(it), robotZ(it))
            } }
            .minByOrNull { abs(robotX(it) - rx) + abs(robotZ(it) - rz) }
            ?: return@update
        val toTgtX: Int = robotX(target) - rx
        val toTgtZ: Int = robotZ(target) - rz
        if (abs(toTgtX) + abs(toTgtZ) > BASIC_ENEMY_SEARCH_RANGE) {
            return@update
        }
        moveTowards(rx, rz, toTgtX, toTgtZ, world)
        shoot(toTgtX, toTgtZ, target)
    }
}

enum class EnemyRobotConfig(
    val robotType: RobotType,
    val baseItem: Item,
    val weaponItem: Item?,
    val update: (EnemyRobot) -> (World) -> Unit
) {
    BASIC(
        robotType = RobotType.SCOUT,
        baseItem = Item(
            ItemType.KENDAL_DYNAMICS_SCOUT,
            ItemVariant.SCOUT_ENEMY
        ),
        weaponItem = Item(ItemType.LASER),
        update = ::updateBasicRobot
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

    private val logicImpl: (World) -> Unit = this.config.update(this)

    init {
        this.alignPosition()
    }

    fun update(world: World) {
        super.update()
        this.logicImpl(world)
    }

}
