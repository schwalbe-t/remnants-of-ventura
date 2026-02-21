
package schwalbe.ventura.server.game

import schwalbe.ventura.bigton.BigtonModule
import schwalbe.ventura.bigton.BigtonModules
import schwalbe.ventura.data.ItemType
import schwalbe.ventura.data.RobotType
import schwalbe.ventura.server.game.attachments.*

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

    ItemType.DIGITAL_RADIO to RobotExtensions(listOf(RADIO_ATTACHMENT_MODULE)),
    ItemType.GPS_RECEIVER to RobotExtensions(listOf(GPS_ATTACHMENT_MODULE))
)

val ROBOT_TYPE_EXT: Map<RobotType, RobotExtensions> = mapOf(
    RobotType.SCOUT to RobotExtensions()
)