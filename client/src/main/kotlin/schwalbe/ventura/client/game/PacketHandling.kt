
package schwalbe.ventura.client.game

import schwalbe.ventura.Version
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.net.*

private enum class VersionDiff { CLIENT_OUTDATED, SERVER_OUTDATED, MATCH }

private fun compareVersions(serverVer: VersionPacket): VersionDiff = when {
    serverVer.major > Version.MAJOR -> VersionDiff.CLIENT_OUTDATED
    serverVer.major < Version.MAJOR -> VersionDiff.SERVER_OUTDATED
    serverVer.minor > Version.MINOR -> VersionDiff.CLIENT_OUTDATED
    serverVer.minor < Version.MINOR -> VersionDiff.SERVER_OUTDATED
    serverVer.patch > Version.PATCH -> VersionDiff.CLIENT_OUTDATED
    serverVer.patch < Version.PATCH -> VersionDiff.SERVER_OUTDATED
    else -> VersionDiff.MATCH
}

fun PacketHandler<Unit>.addVersionCheck(onMismatch: (String) -> Unit) = this
    .onPacket(PacketType.SERVER_VERSION) { sv: VersionPacket, _ ->
        val clientStrVer = "${Version.MAJOR}.${Version.MINOR}.${Version.PATCH}"
        val serverStrVer = "${sv.major}.${sv.minor}.${sv.patch}"
        when (compareVersions(sv)) {
            VersionDiff.CLIENT_OUTDATED -> onMismatch(
                localized()[ERROR_CLIENT_OUTDATED]
                    .replace("{CLIENT}", clientStrVer)
                    .replace("{SERVER}", serverStrVer)
            )
            VersionDiff.SERVER_OUTDATED -> onMismatch(
                localized()[ERROR_SERVER_OUTDATED]
                    .replace("{CLIENT}", clientStrVer)
                    .replace("{SERVER}", serverStrVer)
            )
            else -> {}
        }
    }

fun PacketHandler<Unit>.addErrorLogging() = this
    .onPacket(PacketType.GENERIC_ERROR) { e: GenericErrorPacket, _ ->
        println("[error] ${e.message}")
    }
    .onPacket(PacketType.TAGGED_ERROR) { e: TaggedErrorPacket, _ ->
        println("[error] ${e.name}")
    }

fun PacketHandler<Unit>.addWorldHandling(client: Client) = this
    .onPacket(PacketType.BEGIN_WORLD_CHANGE) { _, _ ->
        client.world?.dispose()
        client.world = null
    }
    .onPacket(PacketType.COMPLETE_WORLD_CHANGE) { info: WorldInfoPacket, _ ->
        client.world?.dispose()
        client.world = World(client, info)
    }
    .onPacket(PacketType.CHUNK_CONTENTS) { c: ChunkContentsPacket, _ ->
        val world: World = client.world ?: return@onPacket
        world.chunks.onChunksReceived(c.chunks)
    }
    .onPacket(PacketType.WORLD_STATE) { s: WorldStatePacket, _ ->
        val world: World = client.world ?: return@onPacket
        world.state.handleReceivedState(s)
    }
    .onPacket(PacketType.VISUAL_EFFECT) { v: VisualEffectPacket, _ ->
        val world: World = client.world ?: return@onPacket
        val renderer = v.vfx.toVfxRenderer(world) ?: return@onPacket
        world.vfx.add(v.relTimestamp, renderer)
    }