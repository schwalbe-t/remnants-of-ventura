
package schwalbe.ventura.server.game

import schwalbe.ventura.bigton.BigtonModule
import schwalbe.ventura.bigton.BigtonModules
import schwalbe.ventura.data.ItemType

val BIGTON_MODULES = BigtonModules<World>()

class RobotAttachmentInfo(
    val addedModules: List<BigtonModule<World>> = listOf(),
    val addedMemory: Long = 0
)

val ATTACHMENT_INFO: Map<ItemType, RobotAttachmentInfo> = mapOf(
    ItemType.PIVOTAL_ME2048 to RobotAttachmentInfo(addedMemory = 2.kb),
    ItemType.PIVOTAL_ME5120 to RobotAttachmentInfo(addedMemory = 5.kb),
    ItemType.PIVOTAL_ME10K  to RobotAttachmentInfo(addedMemory = 10.kb),
    ItemType.PIVOTAL_ME20K  to RobotAttachmentInfo(addedMemory = 20.kb)
)
