
package schwalbe.ventura.server.game.extensions

import schwalbe.ventura.bigton.BigtonModule
import schwalbe.ventura.bigton.BigtonModules
import schwalbe.ventura.data.ItemType
import schwalbe.ventura.data.RobotType
import schwalbe.ventura.server.game.kb

val BIGTON_MODULES = BigtonModules<GameAttachmentContext>()


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
        to RobotExtensions(listOf(makeSonarAttachmentModule(50)))
)

val ROBOT_TYPE_EXT: Map<RobotType, RobotExtensions> = mapOf(
    RobotType.SCOUT to RobotExtensions(listOf(SCOUT_ROBOT_MODULE))
)