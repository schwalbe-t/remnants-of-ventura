
package schwalbe.ventura.server

import schwalbe.ventura.net.*
import kotlinx.coroutines.*
import io.ktor.server.engine.*
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.Frame
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.Netty
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.server.routing.routing
import java.util.concurrent.ConcurrentHashMap
import java.security.KeyStore
import java.util.UUID
import java.io.File

const val MAX_PACKET_PAYLOAD_SIZE: Int = 1024 * 64 // 64 Kib

class Server(
    keyStorePath: String,
    keyStoreAlias: String,
    keyStorePassword: String,
    port: Int
) {

    data class Connection(
        val id: UUID,
        val incoming: PacketInStream,
        val outgoing: PacketOutStream
    )

    val connected = ConcurrentHashMap<UUID, Connection>()
    val notInWorld = ConcurrentHashMap<UUID, Connection>()

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
                val id = UUID.randomUUID()
                val inPackets = PacketInStream(MAX_PACKET_PAYLOAD_SIZE)
                val outPackets = PacketOutStream(this, sendScope)
                val connection = Connection(id, inPackets, outPackets)
                server.onConnect(connection)
                for (frame in incoming) {
                    inPackets.handleBinaryFrame(frame)
                    if (frame is Frame.Close) {
                        break
                    }
                }
                server.onDisconnect(connection)
            }
        }
    }

    init {
        val keyStoreFile = File(keyStorePath)
        val keyStorePass = keyStorePassword.toCharArray()
        val server: Server = this
        val netty = embeddedServer(
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
        netty.start()
    }

    private fun onConnect(connection: Connection) {
        this.connected[connection.id] = connection
        this.notInWorld[connection.id] = connection
    }

    private fun onDisconnect(connection: Connection) {
        this.connected.remove(connection.id)
        this.notInWorld.remove(connection.id)
    }
    
}