
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

        fun interpPos(a: SerVector3, b: SerVector3, n: Float, dest: Vector3f) {
            dest.set(a.toVector3f()).lerp(b.toVector3f(), n)
        }

        fun interpRot(a: Float, b: Float, n: Float): Float
            = a + (b - a) * n

        fun interpAnim(b: AnimationRef<A>, dest: AnimState<A>) {
            if (dest.latestAnim != b) {
                dest.transitionTo(b, 0.25f)
            }
        }

        fun update(deltaTime: Float) {
            this.animation.addTimePassed(deltaTime)
            this.animation.addTransitionTimePassed(
                deltaTime * this.animation.numQueuedTransitions
            )
        }
    }

    class PlayerState : AgentState<PlayerAnim>(PlayerAnim.idle) {
        fun interpolate(a: SharedPlayerInfo, b: SharedPlayerInfo, n: Float) {
            interpPos(a.position, b.position, n, this.position)
            this.rotation = interpRot(a.rotation, b.rotation, n)
            interpAnim(PlayerAnim.fromSharedAnim(b.animation), this.animation)
        }
    }

    class RobotState : AgentState<RobotAnim>(RobotAnim.idle) {
        var baseItem: Item? = null
        var weaponItem: Item? = null
        var weaponRotation: Float = 0f

        fun interpolate(a: SharedRobotInfo, b: SharedRobotInfo, n: Float) {
            interpPos(a.position, b.position, n, this.position)
            this.rotation = interpRot(a.baseRotation, b.baseRotation, n)
            this.weaponRotation = interpRot(
                a.weaponRotation, b.weaponRotation, n
            )
            interpAnim(RobotAnim.fromSharedAnim(b.animation), this.animation)
            this.baseItem = b.baseItem
            this.weaponItem = b.weaponItem
        }
    }

    companion object {
        const val DISPLAY_DELAY_MS: Long = 500L
        const val BUFFERED_PACKET_COUNT: Int = 10
        val PLAYER_NAME_OFFSET: Vector3fc = Vector3f(0f, +2.25f, 0f)
    }


    val received: MutableList<WorldStatePacket> = mutableListOf()
    val lastReceived: WorldStatePacket?
        get() = this.received.lastOrNull()

    var displayedTime: Long = 0
    val interpolated = Interpolated()

    var activeNameDisplays: NameDisplayManager? = null

    fun handleReceivedState(state: WorldStatePacket) {
        this.received.add(state)
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
            .indexOfFirst { it.relTimestamp > this.displayedTime }
        if (afterIdx == -1) { return }
        val beforeIdx: Int = maxOf(afterIdx - 1, 0)
        val before: WorldStatePacket = this.received[beforeIdx]
        val after: WorldStatePacket = this.received[afterIdx]
        val timeDiffMs: Long = after.relTimestamp - before.relTimestamp
        val n: Float = if (timeDiffMs == 0L) { 0f } else {
            (this.displayedTime - before.relTimestamp).toFloat() /
                timeDiffMs.toFloat()
        }
        this.interpolated.players.keys
            .filter { it !in before.players.keys }
            .filter { it !in after.players.keys }
            .forEach { username ->
                this.interpolated.players.remove(username)
                this.activeNameDisplays?.remove(username)
            }
        for ((username, plAfter) in after.players) {
            val plBefore = before.players[username] ?: plAfter
            this.interpolated.players
                .getOrPut(username, ::PlayerState)
                .interpolate(plBefore, plAfter, n)
        }
        this.interpolated.robots.keys
            .filter { it !in before.allRobots.keys }
            .filter { it !in after.allRobots.keys }
            .forEach { robotId ->
                this.interpolated.robots.remove(robotId)
            }
        for ((robotId, rAfter) in after.allRobots) {
            val rBefore = before.allRobots[robotId] ?: rAfter
            val state = this.interpolated.robots.getOrPut(robotId, ::RobotState)
            state.interpolate(rBefore, rAfter, n)
        }
        this.interpolated.ownedRobots = after.ownedRobots
        val dt: Float = client.deltaTime
        this.interpolated.players.values.forEach { it.update(dt) }
        this.interpolated.robots.values.forEach { it.update(dt) }
    }

    fun update(client: Client) {
        if (this.received.isEmpty()) { return }
        this.displayedTime = System.currentTimeMillis()
        this.displayedTime -= client.network.connectedSince ?: 0
        this.displayedTime -= DISPLAY_DELAY_MS
        this.displayedTime = this.displayedTime.coerceIn(
            this.received.first().relTimestamp,
            this.received.last().relTimestamp
        )
        val firstNotDispIdx: Int = this.received
            .indexOfFirst { it.relTimestamp > this.displayedTime }
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
            val baseItem: Item = robot.baseItem ?: continue
            Robot.render(
                pass, robot.position, robot.rotation, robot.weaponRotation,
                robot.animation, baseItem, robot.weaponItem
            )
        }
    }

}
