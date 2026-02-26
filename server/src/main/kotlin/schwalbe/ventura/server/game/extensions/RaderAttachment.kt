
package schwalbe.ventura.server.game.extensions

import kotlinx.serialization.Serializable
import schwalbe.ventura.bigton.BigtonModule
import schwalbe.ventura.bigton.runtime.*
import schwalbe.ventura.data.unitsToUnitIdx
import schwalbe.ventura.server.game.EnemyRobot
import schwalbe.ventura.server.game.PlayerRobot
import schwalbe.ventura.server.game.Robot
import kotlin.math.abs

@Serializable
data class RadarAttachmentState(
    var includeFriendly: Boolean = false,
    var includeRogue: Boolean = true,
    var foundRobots: MutableList<Robot> = mutableListOf(),
    var currentRobot: Robot? = null
) : GameAttachment {
    companion object {
        val TYPE = AttachmentStates.register(::RadarAttachmentState)
    }
}

const val RADAR_NO_ROBOT_MSG: String =
    "There is no current robot to get information about. " +
    "Use 'radarSearch' to discover robots and then use 'radarNext' to " +
    "loop over the results."

fun makeRadarAttachmentModule(
    maxDistTiles: Int
) = BigtonModule(BIGTON_MODULES.functions)
    .withCtxFunction("radarIncludeFriendly", cost = 1, argc = 1) { r, ctx ->
        val state = ctx.robot.attachmentStates[RadarAttachmentState.TYPE]
        r.popStack()?.use {
            state.includeFriendly = it.isTruthy
        }
        BigtonNull.create().use(r::pushStack)
    }
    .withCtxFunction("radarIncludeRogue", cost = 1, argc = 1) { r, ctx ->
        val state = ctx.robot.attachmentStates[RadarAttachmentState.TYPE]
        r.popStack()?.use {
            state.includeRogue = it.isTruthy
        }
        BigtonNull.create().use(r::pushStack)
    }
    .withCtxFunction("radarSearch", cost = 1, argc = 0) { r, ctx ->
        val state = ctx.robot.attachmentStates[RadarAttachmentState.TYPE]
        state.foundRobots.clear()
        state.currentRobot = null
        if (state.includeFriendly) {
            ctx.world.players.values.forEach { player ->
                state.foundRobots.addAll(player.data.deployedRobots.values)
            }
        }
        if (state.includeRogue) {
            state.foundRobots.addAll(ctx.world.data.enemyRobots.values)
        }
        val rx: Int = ctx.robot.tileX
        val rz: Int = ctx.robot.tileZ
        state.foundRobots.removeIf { robot ->
            val frx: Int = robot.tileX
            val frz: Int = robot.tileZ
            val dist: Int = maxOf(abs(frx - rx), abs(frz - rz))
            dist > maxDistTiles
        }
        state.foundRobots.sortBy { robot ->
            val cx: Int = robot.tileX
            val cz: Int = robot.tileZ
            abs(cx - rx) + abs(cz - rz)
        }
        BigtonInt.fromValue(state.foundRobots.size.toLong()).use(r::pushStack)
    }
    .withCtxFunction("radarNext", cost = 1, argc = 0) { r, ctx ->
        val state = ctx.robot.attachmentStates[RadarAttachmentState.TYPE]
        state.currentRobot = state.foundRobots.removeFirstOrNull()
        val hasNext: Boolean = state.currentRobot != null
        BigtonInt.fromValue(if (hasNext) 1 else 0).use(r::pushStack)
    }
    .withCtxFunction("radarRobotDir", cost = 1, argc = 0) { r, ctx ->
        val state = ctx.robot.attachmentStates[RadarAttachmentState.TYPE]
        val current: Robot = state.currentRobot
            ?: return@withCtxFunction r.reportDynError(RADAR_NO_ROBOT_MSG)
        val dx = BigtonInt.fromValue((current.tileX - ctx.robot.tileX).toLong())
        val dz = BigtonInt.fromValue((current.tileZ - ctx.robot.tileZ).toLong())
        arrayOf<BigtonValue?>(dx, dz).useAll {
            BigtonTuple.fromElements(listOf(dx, dz), r).use(r::pushStack)
        }
    }
    .withCtxFunction("radarIsEnemy", cost = 1, argc = 0) { r, ctx ->
        val state = ctx.robot.attachmentStates[RadarAttachmentState.TYPE]
        val current: Robot = state.currentRobot
            ?: return@withCtxFunction r.reportDynError(RADAR_NO_ROBOT_MSG)
        val isEnemy: Boolean = current is EnemyRobot
        BigtonInt.fromValue(if (isEnemy) 1 else 0).use(r::pushStack)
    }
    .withCtxFunction("radarIsFriendly", cost = 1, argc = 0) { r, ctx ->
        val state = ctx.robot.attachmentStates[RadarAttachmentState.TYPE]
        val current: Robot = state.currentRobot
            ?: return@withCtxFunction r.reportDynError(RADAR_NO_ROBOT_MSG)
        val isFriendly: Boolean = current is PlayerRobot
        BigtonInt.fromValue(if (isFriendly) 1 else 0).use(r::pushStack)
    }
