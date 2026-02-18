
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.Client
import schwalbe.ventura.data.*
import schwalbe.ventura.net.*

typealias ItemActionHandler = (item: Item, client: Client) -> Unit

val ITEM_ACTION_HANDLERS: Map<ItemAction, ItemActionHandler> = mapOf(

    ItemAction.DEPLOY_ROBOT to handler@{ item, client ->
        val robotType: RobotType = when (item.type) {
            ItemType.KENDAL_DYNAMICS_SCOUT -> RobotType.SCOUT
            else -> return@handler
        }
        client.network.outPackets?.send(Packet.serialize(
            PacketType.DEPLOY_ROBOT, RobotDeploymentPacket(robotType, item)
        ))
    }

)
