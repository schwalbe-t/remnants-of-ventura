
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
        var (dx, dz) = r.popTuple2Int()
            ?: return@withFunction r.reportDynError(
                "'tileDist' expects a tuple of two integers, but got " +
                "something else instead"
            )
        BigtonInt.fromValue(abs(dx) + abs(dz)).use(r::pushStack)
    }
    .withFunction("toCardinal", cost = 1, argc = 1) { r ->
        var (rdx, rdz) = r.popTuple2Int()
            ?: return@withFunction r.reportDynError(
                "'toCardinal' expects a tuple of two integers, but got " +
                "something else instead"
            )
        val (dx, dz) = tileDirToCardinal(rdx, rdz)
        val vdx = BigtonInt.fromValue(dx)
        val vdz = BigtonInt.fromValue(dz)
        arrayOf<BigtonValue?>(vdx, vdz).useAll {
            BigtonTuple.fromElements(listOf(vdx, vdz), r).use(r::pushStack)
        }
    }
    .withFunction("toLesserCardinal", cost = 1, argc = 1) { r ->
        var (rdx, rdz) = r.popTuple2Int()
            ?: return@withFunction r.reportDynError(
                "'toLesserCardinal' expects a tuple of two integers, but got " +
                "something else instead"
            )
        val (dx, dz) = tileDirToLesserCardinal(rdx, rdz)
        val vdx = BigtonInt.fromValue(dx)
        val vdz = BigtonInt.fromValue(dz)
        arrayOf<BigtonValue?>(vdx, vdz).useAll {
            BigtonTuple.fromElements(listOf(vdx, vdz), r).use(r::pushStack)
        }
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
    ItemType.SHORT_RANGE_RADAR
        to RobotExtensions(listOf(makeRadarAttachmentModule(10))),
    ItemType.MID_RANGE_RADAR
        to RobotExtensions(listOf(makeRadarAttachmentModule(25))),
    ItemType.LONG_RANGE_RADAR
        to RobotExtensions(listOf(makeRadarAttachmentModule(50))),
    ItemType.LASER
        to RobotExtensions(listOf(LASER_ATTACHMENT_MODULE))
)

val ROBOT_TYPE_EXT: Map<RobotType, RobotExtensions> = mapOf(
    RobotType.SCOUT to RobotExtensions(listOf(
        CORE_ROBOT_FUNCTIONS, SCOUT_ROBOT_MODULE
    ))
)