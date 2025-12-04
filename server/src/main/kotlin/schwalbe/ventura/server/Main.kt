
package schwalbe.ventura.server

import schwalbe.ventura.net.*
import schwalbe.ventura.server.database.initDatabase
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

const val DEFAULT_KEYSTORE_PATH: String = "dev-keystore.p12"
const val DEFAULT_KEYSTORE_ALIAS: String = "ventura"
const val DEFAULT_KEYSTORE_PASS: String = "labubu"
const val DEFAULT_PORT: Int = 8443
const val MAX_PACKET_PAYLOAD_SIZE: Int = 1024 * 64 // 64 Kib

fun getKeyStorePath(): String = System.getenv("VENTURA_KEYSTORE_PATH")
    ?: DEFAULT_KEYSTORE_PATH

fun getKeyStoreAlias(): String = System.getenv("VENTURA_KEYSTORE_ALIAS")
    ?: DEFAULT_KEYSTORE_ALIAS

fun getKeyStorePass(): String = System.getenv("VENTURA_KEYSTORE_PASS")
    ?: DEFAULT_KEYSTORE_PASS

fun getPort(): Int = System.getenv("VENTURA_PORT")?.toInt()
    ?: DEFAULT_PORT

data class Connection(
    val id: UUID,
    val inPackets: PacketInStream,
    val outPackets: PacketOutStream
)

val connections = ConcurrentHashMap<UUID, Connection>()

private fun Application.initModule() {
    install(WebSockets) {
        pingPeriodMillis = 15000
        timeoutMillis = 30000
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    val sendScope = CoroutineScope(Dispatchers.IO)
    routing {
        webSocket("/") {
            val id = UUID.randomUUID()
            val inPackets = PacketInStream(MAX_PACKET_PAYLOAD_SIZE)
            val outPackets = PacketOutStream(this, sendScope)
            connections[id] = Connection(id, inPackets, outPackets)
            println("$id connected")
            for (frame in incoming) {
                inPackets.handleBinaryFrame(frame)
                if (frame is Frame.Close) {
                    break
                }
            }
            connections.remove(id)
            println("$id disconnected")
        }
    }
}

fun main() {
    initDatabase()

    val port: Int = getPort()
    val keyStoreFile = File(getKeyStorePath())
    val keyStorePass = getKeyStorePass().toCharArray()
    val server = embeddedServer(
        Netty,
        configure = {
            sslConnector(
                keyStore = keyStoreFile.inputStream().use { stream ->
                    val k = KeyStore.getInstance("PKCS12")
                    k.load(stream, keyStorePass)
                    k
                },
                keyAlias = getKeyStoreAlias(),
                keyStorePassword = { keyStorePass },
                privateKeyPassword = { keyStorePass },
            ) {
                this.port = port
                this.host = "0.0.0.0"
                keyStorePath = keyStoreFile
            }
        },
        module = { initModule() }
    )
    server.start()

    println("Listening on port $port")

    val handler = PacketHandler<Connection>()
    handler.onPacket<EchoPacket>(PacketType.UP_ECHO) { echo, conn ->
        println("[${conn.id}] ${echo.content}")
        conn.outPackets.send(Packet.serialize(PacketType.DOWN_ECHO, echo))
    }

    while (true) {
        for (conn in connections.values) {
            try {
                handler.handleAll(conn.inPackets, conn)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        Thread.sleep(50)
    }
}