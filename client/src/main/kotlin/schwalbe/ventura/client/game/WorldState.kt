
package schwalbe.ventura.client.game

import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.Client
import schwalbe.ventura.engine.gfx.AnimState
import schwalbe.ventura.net.SharedPlayerInfo
import schwalbe.ventura.net.WorldStatePacket
import schwalbe.ventura.net.toVector3f
import org.joml.Vector3f
import org.joml.Vector3fc
import schwalbe.ventura.client.screens.online.NameDisplay

class WorldState {

    data class ReceivedPacket(
        val time: Long, val state: WorldStatePacket
    )

    class Interpolated(
        val players: MutableMap<String, Player> = mutableMapOf()
    ) {
        class Player(
            val position: Vector3f = Vector3f(),
            var rotation: Float = 0f,
            val animation: AnimState<PlayerAnim> = AnimState(PlayerAnim.idle),
            var nameDisplay: Padding? = null
        )
    }

    companion object {
        const val DISPLAY_DELAY_MS: Long = 200L
        val PLAYER_NAME_OFFSET: Vector3fc = Vector3f(0f, +2.25f, 0f)
    }


    val received: MutableList<ReceivedPacket> = mutableListOf()

    var displayedTime: Long = System.currentTimeMillis()
    val interpolated = Interpolated()

    var activePlayerNameDisplay: Stack? = null
        private set

    fun createPlayerNameDisplay(): UiElement {
        for (player in this.interpolated.players.values) {
            player.nameDisplay = null
        }
        val layer = Stack()
        this.activePlayerNameDisplay = layer
        return layer
    }

    fun handleReceivedState(state: WorldStatePacket) {
        val now: Long = System.currentTimeMillis()
        this.received.add(ReceivedPacket(now, state))
    }

    fun updatePlayerNameDisplays(client: Client) {
        for ((username, player) in this.interpolated.players) {
            if (username == client.username) { continue }
            var nameDisplay: UiElement? = player.nameDisplay
            if (nameDisplay == null || nameDisplay.wasDisposed) {
                nameDisplay = NameDisplay.createDisplay(username)
                    .pad()
                player.nameDisplay = nameDisplay
                this.activePlayerNameDisplay?.add(nameDisplay)
            }
            val display: UiElement = nameDisplay.children[0]
            val posWorld = Vector3f(player.position).add(PLAYER_NAME_OFFSET)
            val screenNdc: Vector3f = client.renderer.viewProj
                .transformProject(posWorld)
            val screenNormX: Float = (screenNdc.x() + 1f) / 2f
            val screenNormY: Float = 1f - ((screenNdc.y() + 1f) / 2f)
            val screenPxX: Float = (screenNormX * client.renderer.dest.width) -
                (display.pxWidth / 2f)
            val screenPxY: Float = (screenNormY * client.renderer.dest.height) -
                display.pxHeight
            player.nameDisplay?.withPadding(
                left = screenPxX.px, top = screenPxY.px,
                bottom = 0.px, right = 0.px
            )
            NameDisplay.updateDisplay(display)
        }
    }

    fun update(client: Client) {
        if (this.received.isEmpty()) { return }
        this.displayedTime = System.currentTimeMillis()
        this.displayedTime -= WorldState.DISPLAY_DELAY_MS
        this.displayedTime = this.displayedTime
            .coerceIn(this.received.first().time, this.received.last().time)
        val firstNotDispIdx: Int = this.received
            .indexOfFirst { it.time > this.displayedTime }
        this.received.subList(0, maxOf(firstNotDispIdx - 1, 0)).clear()
        this.updatePlayerNameDisplays(client)
    }

    private fun interpolatePlayerState(
        before: SharedPlayerInfo, after: SharedPlayerInfo, n: Float,
        dest: Interpolated.Player
    ) {
        before.position.toVector3f()
            .lerp(after.position.toVector3f(), n, dest.position)
        dest.rotation = before.rotation + (after.rotation - before.rotation) * n
        val newAnim = PlayerAnim.fromSharedAnim(after.animation)
        if (dest.animation.latestAnim != newAnim) {
            dest.animation.transitionTo(newAnim, 0.25f)
        }
    }

    private fun updateInterpolatedState(deltaTime: Float) {
        this.interpolated.players.values.forEach { player ->
            player.animation.addTimePassed(deltaTime)
            player.animation.addTransitionTimePassed(
                deltaTime * player.animation.numQueuedTransitions
            )
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
        for (username in this.interpolated.players.keys.toList()) {
            if (before.state.players.containsKey(username)) { continue }
            if (after.state.players.containsKey(username)) { continue }
            val player = this.interpolated.players.remove(username) ?: continue
            val nameDisplay: Padding? = player.nameDisplay
            if (nameDisplay != null) {
                this.activePlayerNameDisplay?.without(nameDisplay)
                nameDisplay.disposeTree()
            }
        }
        for ((username, plAfter) in after.state.players) {
            val plBefore = before.state.players[username] ?: plAfter
            val dest = this.interpolated.players
                .getOrPut(username) { Interpolated.Player() }
            this.interpolatePlayerState(plBefore, plAfter, n, dest)
        }
        this.updateInterpolatedState(client.deltaTime)
    }

    fun render(client: Client) {
        this.interpolateWorldState(client)
        for ((username, player) in this.interpolated.players) {
            if (username == client.username) { continue }
            Player.render(
                client, player.position, player.rotation, player.animation
            )
        }
    }

}
