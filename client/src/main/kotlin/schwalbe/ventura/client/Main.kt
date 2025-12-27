
package schwalbe.ventura.client

/*
import schwalbe.ventura.net.*
import kotlinx.coroutines.*
import io.ktor.websocket.Frame
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.URLProtocol
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLContext
import java.security.cert.X509Certificate
import java.security.SecureRandom
import okhttp3.OkHttpClient

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

fun createHttpClient(): HttpClient {
    if (!trustAllCerts()) {
        return HttpClient(OkHttp) { install(WebSockets) }
    }
    println("WARNING! TRUSTING ALL CERTIFICATES")
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

suspend fun main() {
    val socketScope = CoroutineScope(Dispatchers.IO)
    val client: HttpClient = createHttpClient()

    val packetStreamSync = object {}
    var nInPackets: PacketInStream? = null
    var nOutPackets: PacketOutStream? = null

    val handler = PacketHandler<Unit>()
    handler.onPacket<GenericErrorPacket>(PacketType.DOWN_GENERIC_ERROR) { d, _ ->
        println("[error] ${d.message}")
    }
    handler.onPacket<TaggedErrorPacket>(PacketType.DOWN_TAGGED_ERROR) { d, _ ->
        println("[error] ${d.name}")
        when (d) {
            TaggedErrorPacket.INVALID_ACCOUNT_PARAMS -> {
                nOutPackets?.send(Packet.serialize(
                    PacketType.UP_CREATE_SESSION,
                    AccountCredPacket("schwalbe_t", "labubu")
                ))
            }
            else -> {}
        }
    }
    handler.onPacket<Unit>(PacketType.DOWN_CREATE_ACCOUNT_SUCCESS) { _, _ ->
        println("[success] Account creation")
        nOutPackets?.send(Packet.serialize(
            PacketType.UP_CREATE_SESSION,
            AccountCredPacket("schwalbe_t", "labubu")
        ))
    }
    handler.onPacket<SessionTokenPacket>(PacketType.DOWN_CREATE_SESSION_SUCCESS) { d, _ ->
        println("[success] Session creation")
        nOutPackets?.send(Packet.serialize(
            PacketType.UP_LOGIN_SESSION,
            SessionCredPacket("schwalbe_t", d.token)
        ))
    }
    handler.onPacket<Unit>(PacketType.DOWN_LOGIN_SESSION_SUCCESS) { _, _ ->
        println("[success] Session login")
    }

    socketScope.launch {
        client.webSocket(request = {
            url {
                protocol = URLProtocol.WSS
                host = "localhost"
                port = 8443
                pathSegments = listOf()
            }
        }) {
            val inPackets = PacketInStream(MAX_PACKET_PAYLOAD_SIZE)
            val outPackets = PacketOutStream(this, socketScope)
            synchronized(packetStreamSync) {
                nInPackets = inPackets
                nOutPackets = outPackets
            }

            outPackets.send(Packet.serialize(
                PacketType.UP_CREATE_ACCOUNT,
                AccountCredPacket("schwalbe_t", "labubu")
            ))
            
            for (frame in incoming) {
                inPackets.handleBinaryFrame(frame)
                if (frame is Frame.Close) {
                    break
                }
            }

            synchronized(packetStreamSync) {
                nInPackets = null
                nOutPackets = null
            }
        }
    }

    while (true) {
        val inPackets: PacketInStream?
        val outPackets: PacketOutStream?
        synchronized(packetStreamSync) {
            inPackets = nInPackets
            outPackets = nOutPackets
        }
        if (inPackets != null && outPackets != null) {
            handler.handleAll(inPackets, Unit)
        }
        Thread.sleep(1000)
    }
}
*/

import schwalbe.ventura.engine.*
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.engine.ui.*
import org.joml.Vector4f
import java.nio.*
import kotlin.concurrent.thread

fun main() {
    val resLoader = ResourceLoader()
    thread { resLoader.loadQueuedRawLoop() }
    
    loadUiResources(resLoader)
    
    val window = Window("Remnants of Ventura")
    val ui = UiContext(
        output = window.framebuffer,
        defaultFontFamily = "Jetbrains Mono",
        defaultFontSize = 16.px
    )
    
    var onFrame: () -> Unit = {}
    
    resLoader.submit(Resource.fromCallback {
        ui.add(FlatBackground().withRgbColor(255, 0, 0))
        onFrame = {
            ui.update()
            ui.render()
        }
    })
    
    while (!window.shouldClose()) {
        window.beginFrame()
        resLoader.loadQueuedFully()
        
        window.framebuffer.clearColor(Vector4f(1f, 1f, 1f, 1f))
        window.framebuffer.clearDepth(1f)
        onFrame()
        
        window.endFrame()
        Thread.sleep(100)
    }
    window.dispose()
}