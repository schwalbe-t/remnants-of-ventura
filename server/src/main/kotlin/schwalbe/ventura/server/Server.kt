
package schwalbe.ventura.server

import schwalbe.ventura.net.*
import kotlinx.coroutines.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.serializer
import io.ktor.server.engine.*
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.server.routing.routing
import java.util.concurrent.ConcurrentHashMap
import java.security.KeyStore
import kotlin.uuid.Uuid
import java.io.File

const val MAX_PACKET_PAYLOAD_SIZE: Int = 1024 * 64 // 64 Kib

class Server(
    keyStorePath: String,
    keyStoreAlias: String,
    keyStorePassword: String,
    port: Int,
    val worlds: WorldRegistry
) {

    companion object;

    data class Connection(
        val id: Uuid,
        val address: String,
        val incoming: PacketInStream,
        val outgoing: PacketOutStream
    )

    val connected = ConcurrentHashMap<Uuid, Connection>()
    val authorized = ConcurrentHashMap<Uuid, Player>()

    private fun initModule(app: Application) {
        app.install(WebSockets) {
            pingPeriodMillis = 15000
            timeoutMillis = 30000
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
        val sendScope = CoroutineScope(Dispatchers.IO)
        val server: Server = this
        app.routing {
            webSocket("/") {
                val id = Uuid.random()
                val address: String = call.request.local.remoteAddress
                val inPackets = PacketInStream(MAX_PACKET_PAYLOAD_SIZE)
                val outPackets = PacketOutStream(this, sendScope)
                val connection = Connection(id, address, inPackets, outPackets)
                server.onConnect(connection)
                for (frame in incoming) {
                    inPackets.handleBinaryFrame(frame)
                }
                server.onDisconnect(connection)
            }
        }
    }

    private val netty: EmbeddedServer<
        NettyApplicationEngine, NettyApplicationEngine.Configuration
    >

    init {
        val keyStoreFile = File(keyStorePath)
        val keyStorePass = keyStorePassword.toCharArray()
        val server: Server = this
        this.netty = embeddedServer(
            Netty,
            configure = {
                sslConnector(
                    keyStore = keyStoreFile.inputStream().use { stream ->
                        val k = KeyStore.getInstance("PKCS12")
                        k.load(stream, keyStorePass)
                        k
                    },
                    keyAlias = keyStoreAlias,
                    keyStorePassword = { keyStorePass },
                    privateKeyPassword = { keyStorePass },
                ) {
                    this.port = port
                    this.host = "0.0.0.0"
                    this.keyStorePath = keyStoreFile
                }
            },
            module = { server.initModule(this) }
        )
        this.netty.start()
    }

    private fun onConnect(connection: Connection) {
        this.connected[connection.id] = connection
    }

    private fun onDisconnect(connection: Connection) {
        this.connected.remove(connection.id)
        val player = this.authorized.remove(connection.id) ?: return
        this.worlds.handlePlayerDisconnect(player)
        ServerNetwork.releaseAccount(player.username)
    }

    private val unauthorizedHandler: PacketHandler<Connection>
        = PacketHandler.receiveUpPackets<Connection>()

    init {
        val h: PacketHandler<Connection> = this.unauthorizedHandler
        h.onDecodeError = { c, error ->
            c.outgoing.send(Packet.serialize(
                PacketType.GENERIC_ERROR, GenericErrorPacket(error)
            ))
        }
        h.onPacket(PacketType.CREATE_ACCOUNT, this::onAccountCreatePacket)
        h.onPacket(PacketType.CREATE_SESSION, this::onSessionCreatePacket)
        h.onPacket(PacketType.LOGIN_SESSION, this::onSessionLoginPacket)
    }

    fun updateUnauthorized() {
        for (c in this.connected.values) {
            if (this.authorized.containsKey(c.id)) { continue }
            this.unauthorizedHandler.handleAll(c.incoming, c)
        }
    }

    fun stop(gracePeriodMillis: Long, timeoutMillis: Long) {
        this.netty.stop(gracePeriodMillis, timeoutMillis)
    }
}

private fun Server.onAccountCreatePacket(
    data: AccountCredPacket, conn: Server.Connection
) {
    val playerData: ByteArray = Cbor.encodeToByteArray(
        serializer<PlayerData>(), PlayerData.createStartingData(this.worlds)
    )
    val didCreate: Boolean = Account.create(
        data.username, data.password, playerData
    )
    if (!didCreate) {
        conn.outgoing.send(Packet.serialize(
            PacketType.TAGGED_ERROR,
            TaggedErrorPacket.INVALID_ACCOUNT_PARAMS
        ))
        return
    }
    conn.outgoing.send(Packet.serialize(
        PacketType.CREATE_ACCOUNT_SUCCESS, Unit
    ))
}

private fun Server.onSessionCreatePacket(
    data: AccountCredPacket, conn: Server.Connection
) {
    val valid: Boolean = Account.hasMatchingPassword(
        data.username, data.password
    )
    if (!valid) {
        conn.outgoing.send(Packet.serialize(
            PacketType.TAGGED_ERROR,
            TaggedErrorPacket.INVALID_ACCOUNT_CREDS
        ))
        return
    }
    val mayLogin: Boolean = Account.tryApplyLoginCooldown(data.username)
    if (!mayLogin) {
        conn.outgoing.send(Packet.serialize(
            PacketType.TAGGED_ERROR,
            TaggedErrorPacket.SESSION_CREATION_COOLDOWN
        ))
        return
    }
    var token: Uuid?
    do {
        token = Session.create(data.username)
    } while (token == null)
    conn.outgoing.send(Packet.serialize(
        PacketType.CREATE_SESSION_SUCCESS,
        SessionTokenPacket(token)
    ))
}

private fun Server.decodePlayerData(username: String): PlayerData? {
    val rawPlayerData: ByteArray = Account.fetchPlayerData(username)
        ?: return null
    try {
        return Cbor.decodeFromByteArray(
            serializer<PlayerData>(), rawPlayerData
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

private fun Server.onSessionLoginPacket(
    data: SessionCredPacket, conn: Server.Connection
) {
    val expUser: String? = Session.getSessionUser(data.token)
    if (expUser == null || expUser != data.username) {
        conn.outgoing.send(Packet.serialize(
            PacketType.TAGGED_ERROR,
            TaggedErrorPacket.INVALID_SESSION_CREDS
        ))
        return
    }
    if (!ServerNetwork.tryAcquireAccount(data.username)) {
        conn.outgoing.send(Packet.serialize(
            PacketType.TAGGED_ERROR,
            TaggedErrorPacket.ACCOUNT_ALREADY_ONLINE
        ))
        return
    }
    val playerData: PlayerData = this.decodePlayerData(data.username)
        ?: PlayerData.createStartingData(this.worlds)
    val player = Player(data.username, playerData, conn)
    this.authorized[conn.id] = player
    conn.outgoing.send(Packet.serialize(
        PacketType.LOGIN_SESSION_SUCCESS, Unit
    ))
    val world: World = player.getCurrentWorld(this.worlds)
    conn.outgoing.send(Packet.serialize(
        PacketType.BEGIN_WORLD_CHANGE, Unit
    ))
    world.transfer(player)
}