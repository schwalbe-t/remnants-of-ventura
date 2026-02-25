
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

@Serializable
abstract class Robot {

    abstract val type: RobotType
    abstract val item: Item
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
    val rotation: SmoothedFloat = 0f
        .smoothed(response = 10f, epsilon = 0.001f)
    @Transient
    var ticksSinceMoved: Int = 2000
    @Transient
    val movementSteps: MutableList<MovementStep> = mutableListOf()
    val isMoving: Boolean
        get() = this.movementSteps.isNotEmpty()

    fun alignPosition() {
        this.position = SerVector3(
            this.position.x.unitsToUnitIdx() + 0.5f, 0f,
            this.position.z.unitsToUnitIdx() + 0.5f
        )
    }

    fun rotateAlong(direction: Vector3fc) {
        var targetRot = xzVectorAngle(
            MODEL_BASE_ROTATION, Vector3f(direction).normalize()
        )
        this.rotation.target += wrapAngle(targetRot - this.rotation.target)
    }

    fun move(dx: Float, dz: Float, duration: Int) {
        this.rotateAlong(Vector3f(dx, 0f, dz))
        if (duration <= 0) { return }
        this.movementSteps.add(MovementStep(
            dx / duration, dz / duration, duration
        ))
    }

    fun updateMovement() {
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
        this.rotation.update()
        this.ticksSinceMoved = if (this.isMoving) { 0 }
        else { this.ticksSinceMoved + 1 }
    }

    fun getFracHealth(): Float = this.health / this.type.maxHealth

    private val animation: SharedRobotInfo.Animation
        get() = if (this.ticksSinceMoved < 2) { SharedRobotInfo.Animation.MOVE }
            else { SharedRobotInfo.Animation.IDLE }

    fun buildSharedInfo() = SharedRobotInfo(
        this.name, this.item, this.status, this.position, this.rotation.value,
        this.animation
    )

}
