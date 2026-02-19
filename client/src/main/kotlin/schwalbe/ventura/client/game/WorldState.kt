
package schwalbe.ventura.client.game

import schwalbe.ventura.client.screens.online.*
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.client.Client
import schwalbe.ventura.client.RenderPass
import schwalbe.ventura.net.*
import org.joml.Vector3f
import org.joml.Vector3fc
import schwalbe.ventura.data.Item
import kotlin.uuid.Uuid

// The server automatically only sends the world state in a specific radius,
// so there is no reason for any logic to limit rendering / updating here
class WorldState {

    data class ReceivedPacket(
        val time: Long, val state: WorldStatePacket
    )

    class Interpolated(
        val players: MutableMap<String, PlayerState> = mutableMapOf(),
        val robots: MutableMap<Uuid, RobotState> = mutableMapOf(),
        var ownedRobots: Map<Uuid, PrivateRobotInfo> = mapOf()
    )

    open class AgentState<A : Animations<A>>(startingAnim: AnimationRef<A>) {
        val position: Vector3f = Vector3f()
        var rotation: Float = 0f
        val animation: AnimState<A> = AnimState(startingAnim)

        fun interpolate(
            beforePos: SerVector3, afterPos: SerVector3,
            beforeRot: Float, afterRot: Float,
            afterAnim: AnimationRef<A>,
            n: Float
        ) {
            this.position.set(beforePos.toVector3f())
                .lerp(afterPos.toVector3f(), n)
            this.rotation = beforeRot + (afterRot - beforeRot) * n
            if (this.animation.latestAnim != afterAnim) {
                this.animation.transitionTo(afterAnim, 0.25f)
            }
        }

        fun update(deltaTime: Float) {
            this.animation.addTimePassed(deltaTime)
            this.animation.addTransitionTimePassed(
                deltaTime * this.animation.numQueuedTransitions
            )
        }
    }

    class PlayerState : AgentState<PlayerAnim>(PlayerAnim.idle)

    class RobotState : AgentState<RobotAnim>(RobotAnim.dummy) {
        var item: Item? = null
    }

    companion object {
        const val DISPLAY_DELAY_MS: Long = 200L
        const val BUFFERED_PACKET_COUNT: Int = 5
        val PLAYER_NAME_OFFSET: Vector3fc = Vector3f(0f, +2.25f, 0f)
    }


    val received: MutableList<ReceivedPacket> = mutableListOf()
    val lastReceived: WorldStatePacket?
        get() = this.received.lastOrNull()?.state

    var displayedTime: Long = System.currentTimeMillis()
    val interpolated = Interpolated()

    var activeNameDisplays: NameDisplayManager? = null

    fun handleReceivedState(state: WorldStatePacket) {
        val now: Long = System.currentTimeMillis()
        this.received.add(ReceivedPacket(now, state))
    }

    fun updatePlayerNameDisplays(client: Client) {
        val displays = this.activeNameDisplays ?: return
        displays.removeIf { it !in this.interpolated.players.keys }
        for ((username, player) in this.interpolated.players) {
            if (username == client.username) { continue }
            displays.add(username)
            val posWorld = Vector3f(player.position).add(PLAYER_NAME_OFFSET)
            val screenNdc: Vector3f = client.renderer.camViewProj
                .transformProject(posWorld)
            val screenNormX: Float = (screenNdc.x() + 1f) / 2f
            val screenNormY: Float = 1f - ((screenNdc.y() + 1f) / 2f)
            val screenPxX: Float = screenNormX * client.renderer.dest.width
            val screenPxY: Float = screenNormY * client.renderer.dest.height
            displays.update(username, screenPxX, screenPxY)
        }
    }

    private fun interpolateWorldState(client: Client) {
        val afterIdx: Int = this.received
            .indexOfFirst { it.time > this.displayedTime }
        if (afterIdx == -1) { return }
        val beforeIdx: Int = maxOf(afterIdx - 1, 0)
        val before: ReceivedPacket = this.received[beforeIdx]
        val after: ReceivedPacket = this.received[afterIdx]
        val timeDiffMs: Long = after.time - before.time
        val n: Float = if (timeDiffMs == 0L) { 0f } else {
            (this.displayedTime - before.time).toFloat() / timeDiffMs.toFloat()
        }
        this.interpolated.players.keys
            .filter { it !in before.state.players.keys }
            .filter { it !in after.state.players.keys }
            .forEach { username ->
                this.interpolated.players.remove(username)
                this.activeNameDisplays?.remove(username)
            }
        for ((username, plAfter) in after.state.players) {
            val plBefore = before.state.players[username] ?: plAfter
            this.interpolated.players
                .getOrPut(username, ::PlayerState)
                .interpolate(
                    plBefore.position, plAfter.position,
                    plBefore.rotation, plAfter.rotation,
                    PlayerAnim.fromSharedAnim(plAfter.animation),
                    n
                )
        }
        this.interpolated.robots.keys
            .filter { it !in before.state.allRobots.keys }
            .filter { it !in after.state.allRobots.keys }
            .forEach { robotId ->
                this.interpolated.robots.remove(robotId)
            }
        for ((robotId, rAfter) in after.state.allRobots) {
            val rBefore = before.state.allRobots[robotId] ?: rAfter
            val state = this.interpolated.robots.getOrPut(robotId, ::RobotState)
            state.interpolate(
                rBefore.position, rAfter.position,
                rBefore.rotation, rAfter.rotation,
                RobotAnim.dummy, // TODO! replace with shared animation
                n
            )
            state.item = rAfter.item
        }
        this.interpolated.ownedRobots = after.state.ownedRobots
        val dt: Float = client.deltaTime
        this.interpolated.players.values.forEach { it.update(dt) }
        this.interpolated.robots.values.forEach { it.update(dt) }
    }

    fun update(client: Client) {
        if (this.received.isEmpty()) { return }
        this.displayedTime = System.currentTimeMillis()
        this.displayedTime -= DISPLAY_DELAY_MS
        this.displayedTime = this.displayedTime
            .coerceIn(this.received.first().time, this.received.last().time)
        val firstNotDispIdx: Int = this.received
            .indexOfFirst { it.time > this.displayedTime }
        this.received.subList(
            0, maxOf(firstNotDispIdx - BUFFERED_PACKET_COUNT, 0)
        ).clear()
        this.updatePlayerNameDisplays(client)
        this.interpolateWorldState(client)
    }

    fun render(client: Client, pass: RenderPass) {
        for ((username, player) in this.interpolated.players) {
            if (username == client.username) { continue }
            Player.render(
                pass, player.position, player.rotation, player.animation
            )
        }
        for (robot in this.interpolated.robots.values) {
            val item: Item = robot.item ?: continue
            Robot.render(
                pass, robot.position, robot.rotation, robot.animation, item
            )
        }
    }

}
