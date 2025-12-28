
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
    
    val window = Window("Remnants of Ventura", fullscreen = false)
    val ui = UiContext(output = window.framebuffer)
    
    var onFrame: () -> Unit = {}
    
    val jetbrainsMono: Resource<Font> = Font.loadTtf(
        "res/fonts/JetBrainsMono-BoldItalic.ttf"
    )
    resLoader.submit(jetbrainsMono)
    
    resLoader.submit(Resource.fromCallback {
        
        ui.defaultFont = jetbrainsMono()
        ui.add(
            Image().withImage(testImage2()).withSize(fpw, fph),
            layer = -1
        )
        ui.add(Text()
            .withText("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc at tristique lacus, vel tincidunt orci. Pellentesque vitae velit neque. Proin dictum faucibus quam, at mollis risus suscipit ac. Sed posuere ultrices tellus et eleifend. Sed quam eros, aliquet eu pretium ac, elementum porttitor augue. Vestibulum egestas neque ligula, eget fringilla neque tincidunt quis. Sed rhoncus sit amet urna a tempor. Donec ipsum nulla, efficitur sit amet viverra et, fermentum et eros. Nam a quam est. Suspendisse convallis vitae nisl ut sollicitudin. Proin mauris odio, hendrerit vel turpis in, ornare viverra tellus. Fusce placerat nunc et massa aliquet, condimentum rhoncus ex luctus. Fusce pharetra gravida ultrices. Ut egestas sed augue a faucibus. Curabitur vitae turpis tempor, sagittis tellus non, facilisis enim. Curabitur quis rutrum urna, in venenatis ligula.\n\nMeow meow meow")
            .withSize(16.px)
            .withAlignment(Text.Alignment.LEFT)
            .withColor(0, 255, 0)
            .inside(Padding().withPadding(16.px)),
        )
        
        onFrame = {
            ui.update()
            ui.render()
        }
    })
    
    while (!window.shouldClose()) {
        window.beginFrame()
        resLoader.loadQueuedFully()
        
        window.framebuffer.clearColor(Vector4f(1f, 0f, 1f, 1f))
        window.framebuffer.clearDepth(1f)
        onFrame()
        
        window.endFrame()
        Thread.sleep(15)
    }
    window.dispose()
    resLoader.submit(ResourceLoader.stop)
}