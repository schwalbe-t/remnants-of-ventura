
package schwalbe.ventura.client

import schwalbe.ventura.net.*
import kotlinx.coroutines.*
import io.ktor.websocket.Frame
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.URLProtocol
import io.ktor.websocket.close
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLContext
import java.security.cert.X509Certificate
import java.security.SecureRandom
import okhttp3.OkHttpClient

val shouldTrustAllCerts: Boolean
        = System.getenv("VENTURA_TRUST_ALL_CERTS") == "true"

val unsafeAllTrustManager: X509TrustManager = object : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    override fun checkClientTrusted(
        chain: Array<X509Certificate>, authType: String
    ) {}
    override fun checkServerTrusted(
        chain: Array<X509Certificate>, authType: String
    ) {}
}

fun createHttpClient(): HttpClient {
    if (!shouldTrustAllCerts) {
        return HttpClient(OkHttp) { install(WebSockets) }
    }
    println("WARNING! TRUSTING ALL HTTP CERTIFICATES")
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, arrayOf(unsafeAllTrustManager), SecureRandom())
    return HttpClient(OkHttp) {
        engine {
            preconfigured = OkHttpClient.Builder()
                .sslSocketFactory(
                    sslContext.socketFactory, unsafeAllTrustManager
                )
                .hostnameVerifier { _, _ -> true }
                .build()
        }
        install(WebSockets)
    }
}

class NetworkClient {

    sealed interface State
    object Idle : State
    object Connecting : State
    class Connected(
        val address: String,
        val port: Int,
        val session: DefaultClientWebSocketSession,
        val inPackets: PacketInStream,
        val outPackets: PacketOutStream,
        val connectedSince: Long
    ) : State
    class ExceptionError(
        val e: Exception
    ) : State


    val http: HttpClient = createHttpClient()

    val socketScope = CoroutineScope(Dispatchers.IO)

    var state: State = Idle
        get() { synchronized(this) { return field } }
        set(value) { synchronized(this) { field = value } }

    val inPackets: PacketInStream?
        get() = when (val s = this.state) {
            is Connected -> s.inPackets
            else -> null
        }
    val outPackets: PacketOutStream?
        get() = when (val s = this.state) {
            is Connected -> s.outPackets
            else -> null
        }
    val connectedSince: Long?
        get() = when (val s = this.state) {
            is Connected -> s.connectedSince
            else -> null
        }

}

const val MAX_PACKET_PAYLOAD_SIZE: Int = 1024 * 1024 // 1 Mib

fun NetworkClient.connect(targetAddress: String, targetPort: Int) {
    val nc: NetworkClient = this
    val oldState = nc.state
    nc.state = NetworkClient.Connecting
    this.socketScope.launch {
        try {
            if (oldState is NetworkClient.Connected) {
                oldState.session.close()
            }
            nc.http.webSocket(request = {
                url {
                    protocol = URLProtocol.WSS
                    host = targetAddress
                    port = targetPort
                    pathSegments = listOf()
                }
            }) {
                val inPackets = PacketInStream(MAX_PACKET_PAYLOAD_SIZE)
                val outPackets = PacketOutStream(this, socketScope)
                val connection = NetworkClient.Connected(
                    targetAddress, targetPort, this, inPackets, outPackets,
                    connectedSince = System.currentTimeMillis()
                )
                nc.state = connection

                for(frame in incoming) {
                    inPackets.handleBinaryFrame(frame)
                    if (frame is Frame.Close) {
                        break
                    }
                }

                synchronized(nc) {
                    if (nc.state == connection) {
                        nc.state = NetworkClient.Idle
                    }
                }
            }
        } catch (e: Exception) {
            nc.state = NetworkClient.ExceptionError(e)
        }
    }
}

fun NetworkClient.disconnect() {
    val s = this.state
    if (s !is NetworkClient.Connected) { return }
    val nc: NetworkClient = this
    this.socketScope.launch {
        s.session.close()
        nc.state = NetworkClient.Idle
    }
}

fun NetworkClient.clearError() {
    this.state = NetworkClient.Idle
}

fun NetworkClient.handlePackets(handler: PacketHandler<Unit>?) {
    if (handler == null) { return }
    val s = this.state
    if (s !is NetworkClient.Connected) { return }
    handler.handleAll(s.inPackets, Unit)
}

fun NetworkClient.dispose() {
    this.disconnect()
    this.http.close()
}