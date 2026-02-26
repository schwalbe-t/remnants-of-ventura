
package schwalbe.ventura.client.game

import schwalbe.ventura.client.RenderPass
import schwalbe.ventura.client.Renderer
import schwalbe.ventura.data.Item
import schwalbe.ventura.data.VisualEffect
import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.net.toVector3f
import org.joml.*

class VisualEffects {

    data class Renderer(
        val duration: Float,
        val render: (RenderPass, Float) -> Unit
    )

    data class Entry(
        val dispAfter: Long,
        var remTime: Float,
        val renderer: Renderer
    )

    companion object;


    private val queued: MutableList<Entry> = mutableListOf()
    private val rendered: MutableList<Entry> = mutableListOf()

    fun add(relTime: Long, vfx: Renderer) {
        this.queued.add(Entry(dispAfter = relTime, remTime = vfx.duration, vfx))
    }

    fun render(pass: RenderPass, deltaTime: Float, worldState: WorldState) {
        val now = worldState.displayedTime
        this.rendered.addAll(this.queued.filter { it.dispAfter <= now })
        this.queued.removeIf { it.dispAfter <= now }
        this.rendered.forEach {
            val n: Float = it.remTime / it.renderer.duration
            it.renderer.render(pass, n)
            it.remTime -= deltaTime
        }
        this.rendered.removeIf { it.remTime <= 0f }
    }

}


private const val OUTLINE_THICKNESS: Float = 0.015f

// offset from the base of the laser object to the laser ray coming out of it
private val LASER_ORIGIN_OFFSET: Vector3fc = Vector3f(0f, +0.4f, +0.45f)
private val LASER_RAY_FORWARD: Vector3fc = Vector3f(0f, 0f, +1f)
private val LASER_RAY: Resource<Model<StaticAnim>>
    = Model.loadFile("res/vfx/laser_ray.glb", Renderer.meshProperties)

private fun laserRenderer(
    ray: VisualEffect.LaserRay, world: World
): VisualEffects.Renderer? {
    val originRobot: WorldState.RobotState
        = world.state.interpolated.robots[ray.originRobot] ?: return null
    val originRobotBaseItem: Item = originRobot.baseItem ?: return null
    val weaponTransf = Robot.weaponTransform(
        originRobotBaseItem.type, originRobot.position,
        originRobot.weaponRotation
    )
    val rayOrigin: Vector3f = weaponTransf
        .transformPosition(Vector3f(LASER_ORIGIN_OFFSET))
    val rayDestination = ray.towards.toVector3f()
    val rayDirection = Vector3f().set(rayDestination).sub(rayOrigin).normalize()
    val rayLength: Float = rayOrigin.distance(rayDestination)
    val rayRotation = Quaternionf().rotationTo(LASER_RAY_FORWARD, rayDirection)
    val rayTransform = Matrix4f()
        .translate(rayOrigin)
        .rotate(rayRotation)
        .scale(1f, 1f, rayLength)
    val rayInstances = listOf(rayTransform)
    return VisualEffects.Renderer(
        duration = 0.25f,
        render = { pass, _ ->
            pass.renderOutline(
                LASER_RAY(), OUTLINE_THICKNESS, null, rayInstances
            )
            pass.renderGeometry(LASER_RAY(), null, rayInstances)
        }
    )
}

fun VisualEffects.Companion.submitResources(resLoader: ResourceLoader) {
    resLoader.submit(LASER_RAY)
}

fun VisualEffect.toVfxRenderer(world: World): VisualEffects.Renderer? {
    return when (this) {
        is VisualEffect.LaserRay -> laserRenderer(this, world)
    }
}