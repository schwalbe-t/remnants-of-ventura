
package schwalbe.ventura.client.game

import schwalbe.ventura.client.Client
import schwalbe.ventura.net.*
import schwalbe.ventura.data.ConstWorldInfo

fun PacketHandler<Unit>.addErrorLogging() = this
    .onPacket(PacketType.GENERIC_ERROR) { e: GenericErrorPacket, _ ->
        println("[error] ${e.message}")
    }
    .onPacket(PacketType.TAGGED_ERROR) { e: TaggedErrorPacket, _ ->
        println("[error] ${e.name}")
    }

fun PacketHandler<Unit>.addWorldHandling(client: Client) = this
    .onPacket(PacketType.BEGIN_WORLD_CHANGE) { _, _ ->
        client.world = null
    }
    .onPacket(PacketType.COMPLETE_WORLD_CHANGE) { entry: WorldEntryPacket, _ ->
        val world = World()
        world.player.position.set(entry.position.toVector3f())
        client.world = world
        client.network.outPackets?.send(Packet.serialize(
            PacketType.REQUEST_WORLD_INFO, Unit
        ))
    }
    .onPacket(PacketType.CONST_WORLD_INFO) { i: ConstWorldInfo, _ ->
        client.renderer.config = i.rendererConfig
    }
    .onPacket(PacketType.CHUNK_CONTENTS) { c: ChunkContentsPacket, _ ->
        val world: World = client.world ?: return@onPacket
        world.chunks.handleReceivedChunks(c)
    }
    .onPacket(PacketType.WORLD_STATE) { s: WorldStatePacket, _ ->
        val world: World = client.world ?: return@onPacket
        world.state.handleReceivedState(s)
    }