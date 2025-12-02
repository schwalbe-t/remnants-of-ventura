
package schwalbe.ventura.client

import schwalbe.ventura.net.*
import kotlinx.coroutines.*
import io.ktor.network.sockets.*
import io.ktor.network.selector.ActorSelectorManager
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLContext
import java.security.cert.X509Certificate
import io.ktor.network.tls.tls

const val MAX_PACKET_PAYLOAD_SIZE: Int = 1024 * 1024 // 1 Mib

fun trustAllCerts(): Boolean = System.getenv("VENTURA_TRUST_ALL_CERTS") == "1"

val unsafeAllTrustManager: X509TrustManager = object : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    override fun checkClientTrusted(
        chain: Array<X509Certificate>, authType: String
    ) {}
    override fun checkServerTrusted(
        chain: Array<X509Certificate>, authType: String
    ) {}
}

suspend fun main() {
    val selector = ActorSelectorManager(Dispatchers.IO)
    val coroutineScope = CoroutineScope(Dispatchers.IO)

    val rawSocket = aSocket(selector).tcp()
        .connect(InetSocketAddress("localhost", 23348))

    val socket: Socket = if (trustAllCerts()) {
        println("TRUSTING ALL CERTIFICATES")
        rawSocket.tls(Dispatchers.IO, unsafeAllTrustManager)
    } else {
        rawSocket.tls(Dispatchers.IO)
    }

    val packetIn = PacketInStream(socket, MAX_PACKET_PAYLOAD_SIZE)
    val packetOut = PacketOutStream(socket, coroutineScope)
    val packetHandler = PacketHandler<Unit>()
    packetHandler.onPacket<EchoPacket>(PacketType.DOWN_ECHO) { echo, _ ->
        println(echo.content)
    }

    var n: Int = 0

    try {
        while (true) {
            packetHandler.handleAll(packetIn, Unit)
            packetOut.send(Packet.serialize(
                PacketType.UP_ECHO, EchoPacket("Hello, Ventura! ($n)")
            ))
            n += 1
            Thread.sleep(1000)
        }
    } catch(e: Exception) {
        e.printStackTrace()
    }

    socket.close()
    coroutineScope.cancel()
    selector.close()
}