
package schwalbe.ventura.server

import schwalbe.ventura.net.*
import io.ktor.network.tls.*
import kotlinx.coroutines.*
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.security.KeyStore
import java.io.FileInputStream

const val DEFAULT_KEYSTORE_PATH: String = "dev-keystore.p12"
const val DEFAULT_KEYSTORE_PASS: String = "labubu"
const val DEFAULT_PORT: Int = 23348
const val MAX_PACKET_PAYLOAD_SIZE: Int = 1024 * 64 // 64 Kib

fun getKeyStorePath(): String = System.getenv("VENTURA_KEYSTORE_PATH")
    ?: DEFAULT_KEYSTORE_PATH

fun getKeyStorePass(): String = System.getenv("VENTURA_KEYSTORE_PASS")
    ?: DEFAULT_KEYSTORE_PASS

fun getPort(): Int = System.getenv("VENTURA_PORT")?.toInt()
    ?: DEFAULT_PORT

fun buildTlsConfig(): TLSConfig {
    val keyStorePass = getKeyStorePass().toCharArray()
    val keyStore = KeyStore.getInstance("PKCS12")
    keyStore.load(FileInputStream(getKeyStorePath()), keyStorePass)
    val configBuilder = TLSConfigBuilder()
    configBuilder.addKeyStore(keyStore, keyStorePass)
    return configBuilder.build()
}

data class Connection(
    val id: UUID,
    val socket: Socket,
    val packetIn: PacketInStream,
    val packetOut: PacketOutStream
)

suspend fun main() {
    val selector = ActorSelectorManager(Dispatchers.IO)
    val coroutineScope = CoroutineScope(Dispatchers.IO)

    val tlsConfig: TLSConfig = buildTlsConfig()
    val port: Int = getPort()
    val serverSocket = aSocket(selector).tcp()
        .bind(InetSocketAddress("0.0.0.0", port))

    println("Listening on port $port")

    val connections = ConcurrentHashMap<UUID, Connection>()
    while (true) {
        try {
            val rawSocket = serverSocket.accept()
            val s: Socket = rawSocket.tls(Dispatchers.IO, tlsConfig)
            val id: UUID = UUID.randomUUID()
            val packetIn = PacketInStream(s, MAX_PACKET_PAYLOAD_SIZE)
            val packetOut = PacketOutStream(s, coroutineScope)
            connections[id] = Connection(id, s, packetIn, packetOut)
            println("$id connected")
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    val handler = PacketHandler<Connection>()
    handler.onPacket<EchoPacket>(PacketType.UP_ECHO) { echo, conn ->
        println("[${conn.id}] ${echo.content}")
        conn.packetOut.send(Packet.serialize(PacketType.DOWN_ECHO, echo))
    }

    while (true) {
        for (conn in connections.values) {
            try {
                handler.handleAll(conn.packetIn, conn)
            } catch (e: Exception) {
                e.printStackTrace()
                conn.socket.close()
                connections.remove(conn.id)
                println("${conn.id} disconnected")
            }
        }
        Thread.sleep(50)
    }

    // serverSocket.close()
    // connections.values.forEach { c -> c.socket.close() }
    // selector.close()
    // coroutineScope.cancel()
}