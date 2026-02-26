
package schwalbe.ventura.server.game

import schwalbe.ventura.data.*
import schwalbe.ventura.net.*
import schwalbe.ventura.utils.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.joml.Vector3f
import org.joml.Vector3fc
import kotlin.math.roundToLong
import kotlin.uuid.Uuid

val Int.kb: Long
    get() = this * 1024L

val Double.kb: Long
    get() = (this * 1024.0).roundToLong()

private fun SmoothedFloat.rotateTowards(newAngle: Float) {
    this.target += wrapAngle(newAngle - this.target)
}

@Serializable
abstract class Robot {

    var tileX: Int = 0
        private set
    var tileZ: Int = 0
        private set

    abstract val type: RobotType
    abstract val baseItem: Item
    abstract val weaponItem: Item?
    abstract var position: SerVector3

    abstract val status: RobotStatus
    abstract val name: String
    abstract var health: Float

    data class MovementStep(val dx: Float, val dz: Float, var remTicks: Int)

    companion object {
        val MODEL_BASE_ROTATION: Vector3fc = Vector3f(0f, 0f, +1f)
    }

    val id: Uuid = Uuid.random()
    @Transient
    val baseRotation: SmoothedFloat = 0f
        .smoothed(response = 10f, epsilon = 0.001f)
    @Transient
    val weaponRotation: SmoothedFloat = 0f
        .smoothed(response = 10f, epsilon = 0.001f)
    @Transient
    var ticksSinceMoved: Int = 2000
    @Transient
    val movementSteps: MutableList<MovementStep> = mutableListOf()
    val isMoving: Boolean
        get() = this.movementSteps.isNotEmpty()

    fun alignPosition() {
        this.tileX = this.position.x.unitsToUnitIdx()
        this.tileZ = this.position.z.unitsToUnitIdx()
        this.position = SerVector3(
            this.tileX + 0.5f, this.position.y,
            this.tileZ + 0.5f
        )
    }

    fun rotateBaseAlong(direction: Vector3fc) {
        this.baseRotation.rotateTowards(xzVectorAngle(
            MODEL_BASE_ROTATION, Vector3f(direction).normalize()
        ))
    }

    fun rotateWeaponAlong(direction: Vector3fc) {
        this.weaponRotation.rotateTowards(xzVectorAngle(
            MODEL_BASE_ROTATION, Vector3f(direction).normalize()
        ))
    }

    fun move(dx: Float, dz: Float, duration: Int) {
        this.rotateBaseAlong(Vector3f(dx, 0f, dz))
        if (duration <= 0) {
            this.position = SerVector3(
                this.position.x + dx, this.position.y,
                this.position.z + dz
            )
            this.alignPosition()
            return
        }
        this.tileX = (this.position.x + dx).unitsToUnitIdx()
        this.tileZ = (this.position.z + dz).unitsToUnitIdx()
        this.movementSteps.add(MovementStep(
            dx / duration, dz / duration, duration
        ))
    }

    fun update() {
        val movementStep: MovementStep? = this.movementSteps.firstOrNull()
        if (movementStep != null) {
            this.position = SerVector3(
                this.position.x + movementStep.dx, 0f,
                this.position.z + movementStep.dz
            )
            movementStep.remTicks -= 1
            if (movementStep.remTicks <= 0) {
                this.movementSteps.removeFirst()
                this.alignPosition()
            }
        }
        this.baseRotation.update()
        this.weaponRotation.update()
        this.ticksSinceMoved = if (this.isMoving) { 0 }
        else { this.ticksSinceMoved + 1 }
    }

    fun getFracHealth(): Float = this.health / this.type.maxHealth

    private val animation: SharedRobotInfo.Animation
        get() = if (this.ticksSinceMoved < 2) { SharedRobotInfo.Animation.MOVE }
            else { SharedRobotInfo.Animation.IDLE }

    fun buildSharedInfo() = SharedRobotInfo(
        this.name,
        this.baseItem, this.weaponItem,
        this.status, this.position,
        this.baseRotation.value, this.weaponRotation.value,
        this.animation
    )

}
