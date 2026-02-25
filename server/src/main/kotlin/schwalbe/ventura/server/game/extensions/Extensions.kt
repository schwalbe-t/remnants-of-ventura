
package schwalbe.ventura.server.game.extensions

import schwalbe.ventura.bigton.BigtonModule
import schwalbe.ventura.bigton.BigtonModules
import schwalbe.ventura.bigton.runtime.*
import schwalbe.ventura.data.ItemType
import schwalbe.ventura.data.RobotType
import schwalbe.ventura.data.unitsToUnitIdx
import schwalbe.ventura.server.game.kb
import kotlin.math.abs

val BIGTON_MODULES = BigtonModules<GameAttachmentContext>()

val CORE_ROBOT_FUNCTIONS = BigtonModule(BIGTON_MODULES.functions)
    .withFunction("tileDist", cost = 1, argc = 1) { r ->
        var dist: Long = r.popStack()
            ?.use {
                if (it !is BigtonTuple || it.length != 2) { return@use null }
                val dx: BigtonValue = it[0]
                val dz: BigtonValue = it[1]
                if (dx !is BigtonInt || dz !is BigtonInt) { return@use null }
                arrayOf<BigtonValue?>(dx, dz).useAll {
                    abs(dx.value) + abs(dz.value)
                }
            }
            ?: return@withFunction r.reportDynError(
                "'manhattanDist' expects a tuple of two integers, but got " +
                "something else instead"
            )
        BigtonInt.fromValue(dist).use(r::pushStack)
    }
    .withCtxFunction("playerDir", cost = 1, argc = 0) { r, ctx ->
        val playerPos = ctx.player.data.worlds.last().state.position
        val playerTx: Long = playerPos.x.unitsToUnitIdx().toLong()
        val playerTz: Long = playerPos.z.unitsToUnitIdx().toLong()
        val robotPos = ctx.robot.position
        val robotTx: Long = robotPos.x.unitsToUnitIdx().toLong()
        val robotTz: Long = robotPos.z.unitsToUnitIdx().toLong()
        val dx = BigtonInt.fromValue(playerTx - robotTx)
        val dz = BigtonInt.fromValue(playerTz - robotTz)
        arrayOf<BigtonValue?>(dx, dz).useAll {
            BigtonTuple.fromElements(listOf(dx, dz), r).use(r::pushStack)
        }
    }


class RobotExtensions(
    val addedModules: List<BigtonModule<GameAttachmentContext>> = listOf(),
    val addedMemory: Long = 0
)

val ATTACHMENT_EXT: Map<ItemType, RobotExtensions> = mapOf(
    ItemType.PIVOTAL_ME2048 to RobotExtensions(addedMemory = 2.kb),
    ItemType.PIVOTAL_ME5120 to RobotExtensions(addedMemory = 5.kb),
    ItemType.PIVOTAL_ME10K  to RobotExtensions(addedMemory = 10.kb),
    ItemType.PIVOTAL_ME20K  to RobotExtensions(addedMemory = 20.kb),

    ItemType.DIGITAL_RADIO
        to RobotExtensions(listOf(RADIO_ATTACHMENT_MODULE)),
    ItemType.GPS_RECEIVER
        to RobotExtensions(listOf(GPS_ATTACHMENT_MODULE)),
    ItemType.SHORT_RANGE_SONAR
        to RobotExtensions(listOf(makeSonarAttachmentModule(5))),
    ItemType.MID_RANGE_SONAR
        to RobotExtensions(listOf(makeSonarAttachmentModule(20))),
    ItemType.LONG_RANGE_SONAR
        to RobotExtensions(listOf(makeSonarAttachmentModule(50))),
    ItemType.LASER
        to RobotExtensions(listOf(LASER_ATTACHMENT_MODULE))
)

val ROBOT_TYPE_EXT: Map<RobotType, RobotExtensions> = mapOf(
    RobotType.SCOUT to RobotExtensions(listOf(
        CORE_ROBOT_FUNCTIONS, SCOUT_ROBOT_MODULE
    ))
)