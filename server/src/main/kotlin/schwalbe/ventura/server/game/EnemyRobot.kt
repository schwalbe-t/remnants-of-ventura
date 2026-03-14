
package schwalbe.ventura.server.game

import org.joml.Vector3f
import schwalbe.ventura.data.*
import schwalbe.ventura.net.SerVector3
import schwalbe.ventura.utils.sign
import kotlin.math.abs

class LootTable(vararg val entries: Entry) {
    data class Entry(val generate: () -> Item?)

    fun generateLoot(): List<Item>
        = this.entries.mapNotNull { it.generate() }
}

fun Item.toLoot()
        = LootTable.Entry { this }

fun LootTable.Entry.withChance(chance: Double): LootTable.Entry
    = LootTable.Entry { if (Math.random() < chance) this.generate() else null }

fun Iterable<Pair<Double, LootTable.Entry>>.one() = LootTable.Entry {
    val sum = this.sumOf { it.first }
    var acc: Double = Math.random()
    for (entry in this) {
        acc -= entry.first / sum
        if (acc > 0.0) { continue }
        return@Entry entry.second.generate()
    }
    null
}


const val BASIC_ENEMY_SEARCH_RANGE: Int = 30
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
        val collX: Boolean = world.tileIsOccupied(rx + sign(toTgtX), rz)
        val collZ: Boolean = world.tileIsOccupied(rx, rz + sign(toTgtZ))
        val rMoveX: Int = if (collX) 0 else toTgtX
        val rMoveZ: Int = if (collZ) 0 else toTgtZ
        val (dx, dz) = if (rMoveZ == 0) { sign(rMoveX) to 0 }
            else { 0 to sign(rMoveZ) }
        if (dx == 0 && dz == 0) { return }
        robot.move(dx.toFloat(), dz.toFloat(), BASIC_ENEMY_MOVEMENT_SPEED)
    }
    var shootCooldown: Long = 0
    fun shoot(toTgtX: Int, toTgtZ: Int, target: PlayerRobot, world: World) {
        if (shootCooldown > 0) {
            shootCooldown -= 1
            return
        }
        if (!canShoot(toTgtX, toTgtZ, BASIC_ENEMY_ATTACK_RANGE)) { return }
        val toTgt = Vector3f(toTgtX.toFloat(), 0f, toTgtZ.toFloat())
        robot.rotateWeaponAlong(toTgt)
        target.health -= BASIC_ENEMY_ATTACK_DAMAGE
        shootCooldown = BASIC_ENEMY_ATTACK_COOLDOWN
        val vfx = VisualEffect.LaserRay(
            robot.id,
            towards = SerVector3(
                target.position.x, target.position.y + 0.25f,
                target.position.z
            )
        )
        world.broadcastVfx(vfx, origin = robot.position)
    }
    return update@{ world ->
        val target: PlayerRobot = world.players.values
            .flatMap { it.data.deployedRobots.values }
            .filter { world.data.static.world.peaceAreas.none { area ->
                area.contains(it.tileX, it.tileZ)
            } }
            .minByOrNull {
                abs(it.tileX - robot.tileX) + abs(it.tileZ - robot.tileZ)
            }
            ?: return@update
        val toTgtX: Int = target.tileX - robot.tileX
        val toTgtZ: Int = target.tileZ - robot.tileZ
        if (abs(toTgtX) + abs(toTgtZ) > BASIC_ENEMY_SEARCH_RANGE) {
            return@update
        }
        moveTowards(robot.tileX, robot.tileZ, toTgtX, toTgtZ, world)
        shoot(toTgtX, toTgtZ, target, world)
    }
}

enum class EnemyRobotConfig(
    val robotType: RobotType,
    val baseItem: Item,
    val weaponItem: Item?,
    val update: (EnemyRobot) -> (World) -> Unit,
    val lootTable: LootTable
) {
    BASIC(
        robotType = RobotType.SCOUT,
        baseItem = Item(
            ItemType.KENDAL_DYNAMICS_SCOUT,
            ItemVariant.SCOUT_ENEMY
        ),
        weaponItem = Item(ItemType.LASER),
        update = ::updateBasicRobot,
        lootTable = LootTable(
            listOf(
                0.6 to Item(ItemType.BIGTON_1030).toLoot(),
                0.5 to Item(ItemType.BIGTON_1050).toLoot(),
                0.4 to Item(ItemType.BIGTON_1070).toLoot(),
                0.3 to Item(ItemType.BIGTON_2030).toLoot(),
                0.2 to Item(ItemType.BIGTON_2050).toLoot(),
                0.1 to Item(ItemType.BIGTON_2070).toLoot()
            ).one()
                .withChance(0.75),
            listOf(
                0.66 to Item(ItemType.SHORT_RANGE_RADAR).toLoot(),
                0.22 to Item(ItemType.MID_RANGE_RADAR).toLoot(),
                0.11 to Item(ItemType.LONG_RANGE_RADAR).toLoot()
            ).one()
                .withChance(0.5),
            Item(ItemType.LASER).toLoot()
                .withChance(0.33)
        )
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
