
package schwalbe.ventura.client.game

import schwalbe.ventura.client.Client
import schwalbe.ventura.net.*
import schwalbe.ventura.net.PacketType.*
import schwalbe.ventura.data.ConstWorldInfo

fun PacketHandler<Unit>.addErrorLogging() = this
    .onPacket(DOWN_GENERIC_ERROR) { e: GenericErrorPacket, _ ->
        println("[error] ${e.message}")
    }
    .onPacket(DOWN_TAGGED_ERROR) { e: TaggedErrorPacket, _ ->
        println("[error] ${e.name}")
    }

fun PacketHandler<Unit>.addWorldHandling(client: Client) = this
    .onPacket(DOWN_BEGIN_WORLD_CHANGE) { _: Unit, _ ->
        client.world = null
    }
    .onPacket(DOWN_COMPLETE_WORLD_CHANGE) { _: Unit, _ ->
        client.world = World()
        client.network.outPackets?.send(Packet.serialize(
            UP_REQUEST_WORLD_INFO, Unit
        ))
    }
    .onPacket(DOWN_CONST_WORLD_INFO) { i: ConstWorldInfo, _ ->
        client.renderer.config = i.rendererConfig
    }
    .onPacket(DOWN_CHUNK_CONTENTS) { c: ChunkContentsPacket, _ ->
        val world: World = client.world ?: return@onPacket
        world.chunks.handleReceivedChunks(c)
    }
    .onPacket(DOWN_WORLD_STATE) { s: WorldStatePacket, _ ->
        val world: World = client.world ?: return@onPacket
        world.state.handleReceivedState(s)
    }