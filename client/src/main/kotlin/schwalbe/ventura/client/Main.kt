
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
import schwalbe.ventura.engine.input.*
import org.joml.*
import java.nio.*
import kotlin.concurrent.thread

object TestModelVert : VertShaderDef<TestModelVert> {
    override val path = "shaders/test.vert.glsl"
    
    val localTransform = mat4("uLocalTransform")
    val modelTransform = mat4("uModelTransform")
    val viewProjection = mat4("uViewProjection")
    val jointTransforms = mat4Arr("uJointTransforms", 64)
}

object TestModelFrag : FragShaderDef<TestModelFrag> {
    override val path = "shaders/test.frag.glsl"
    
    val texture = sampler2D("uTexture")
}

object TestModelAnim : Animations<TestModelAnim> {
    val floss = anim("floss")
    val idle = anim("idle")
    val ride = anim("ride")
    val swim = anim("swim")
    val walk = anim("walk")
}

fun main() {
    val resLoader = ResourceLoader()
    thread { resLoader.loadQueuedRawLoop() }
    
    loadUiResources(resLoader)
    
    val window = Window("Remnants of Ventura", fullscreen = false)

    val dddOut = Framebuffer()
    dddOut.attachColor(Texture(
        16, 16, Texture.Filter.NEAREST, Texture.Format.RGBA8, samples = 4
    ))
    dddOut.attachDepth(Texture(
        16, 16, Texture.Filter.NEAREST, Texture.Format.DEPTH24, samples = 4
    ))

    val uiOut = Framebuffer()
    uiOut.attachColor(Texture(
        16, 16, Texture.Filter.NEAREST, Texture.Format.RGBA8
    ))

    val ui = UiContext(uiOut, window.inputEvents)
    
    var onFrame: (deltaTime: Float) -> Unit = {}
    
    val jetbrainsMono: Resource<Font> = Font.loadTtf(
        "res/fonts/JetBrainsMonoNL-SemiBold.ttf"
    )
    val testImage: Resource<Texture> = Texture.loadImage(
        "res/test2.png", Texture.Filter.LINEAR
    )
    resLoader.submitAll(jetbrainsMono, testImage)
    
    val testModelAnim = AnimState(TestModelAnim.idle)
    val testModel: Resource<Model<TestModelAnim>> = Model.loadFile(
        "res/test.glb",
        listOf(
            Model.Property.POSITION,
            Model.Property.NORMAL,
            Model.Property.UV,
            Model.Property.BONE_IDS_BYTE,
            Model.Property.BONE_WEIGHTS
        ),
        TestModelAnim
    )
    val testModelShader: Resource<Shader<TestModelVert, TestModelFrag>>
        = Shader.loadGlsl(TestModelVert, TestModelFrag)
    resLoader.submitAll(testModel, testModelShader)
    
    resLoader.submit(Resource.fromCallback {
        
        ui.defaultFont = jetbrainsMono()
        ui.defaultFontSize = 16.px
        ui.defaultFontColor = Vector4f(0.9f, 0.9f, 0.9f, 1f)
        val copypasta = "Crazy? I Was Crazy Once. They Locked Me In A Room. A Rubber Room. A Rubber Room With Rats. And Rats Make Me Crazy. "
            .repeat(40)
        val codeSettings = CodeEditingSettings(
            paired = listOf(
                "()",
                "{}",
                "\"\""
            ),
            autoIndent = true
        )
        ui.add(Axis.column()
            .add(80.vh, Stack()
                .add(BlurBackground().withRadius(20))
                .add(FlatBackground()
                    .withColor(13, 16, 22, 128)
                )
                .add(TextInput()
                    .withMultilineInput(true)
                    .withContent(text("", 16.px).withColor(186, 184, 178))
                    // .withDisplayedText { value -> "●".repeat(value.length) }
                    .withDisplayedSpans { text: String ->
                        var spans: MutableList<Span> = mutableListOf()
                        val keyword = "sigma"
                        val keywordColor = Vector4f(0f, 255f, 0f, 255f)
                            .div(255f)
                        var i = 0
                        while (i < text.length) {
                            var next = text.indexOf(keyword, i)
                            if (next == -1) {
                                spans.add(Span(text.substring(i)))
                                break
                            }
                            spans.add(Span(text.substring(i, next)))
                            spans.add(Span(keyword, keywordColor))
                            i = next + keyword.length
                        }
                        spans
                    }
                    .withCodeTypedHandler(codeSettings)
                    .withCodeDeletedHandler(codeSettings)
                    .wrapScrolling()
                    .pad(20.px)
                )
                .wrapBorderRadius(20.px)
                .pad(5.vmin)
            )
            .add(20.vh, BlurBackground().withRadius(20))
        )

        onFrame = { deltaTime ->
            if (!testModelAnim.isTransitioning && Key.W.isPressed) {
                testModelAnim.transitionTo(TestModelAnim.walk, 0.35f)
            }
            if (!testModelAnim.isTransitioning && Key.I.isPressed) {
                testModelAnim.transitionTo(TestModelAnim.idle, 0.35f)
            }
            if (!testModelAnim.isTransitioning && Key.F.isPressed) {
                testModelAnim.transitionTo(TestModelAnim.floss, 0.5f)
            }
            if (!testModelAnim.isTransitioning && Key.S.isPressed) {
                testModelAnim.transitionTo(TestModelAnim.swim, 0.5f)
            }
            testModelAnim.addTimePassed(deltaTime)
            
            // blitTexture(testImage(), out)
            val shader: Shader<TestModelVert, TestModelFrag> = testModelShader()
            shader[TestModelVert.modelTransform] = Matrix4f()
            shader[TestModelVert.viewProjection] = Matrix4f()
                .setPerspective(
                    Math.PI.toFloat() / 2f,
                    dddOut.width.toFloat() / dddOut.height.toFloat(),
                    0.1f, 100f
                )
                .lookAt(
                    +1.0f,  +1.0f,  +1.0f,
                     0.0f,  +1.0f,   0.0f,
                     0.0f,   1.0f,   0.0f
                )
            testModel().render(
                shader, dddOut,
                TestModelVert.localTransform, TestModelFrag.texture,
                TestModelVert.jointTransforms,
                testModelAnim
            )
        }
    })
    
    var lastFrameTime: Long = System.nanoTime()
    while (!window.shouldClose()) {
        window.beginFrame()
        resLoader.loadQueuedFully()
        
        val now: Long = System.nanoTime()
        val deltaTimeNanos: Long = now - lastFrameTime
        val deltaTime: Float = (deltaTimeNanos.toDouble() * 0.000_000_001)
            .toFloat()
        lastFrameTime = now
        
        dddOut.resize(window.framebuffer.width, window.framebuffer.height)
        uiOut.resize(window.framebuffer.width, window.framebuffer.height)

        ui.captureInput()
        window.flushInputEvents()

        dddOut.clearColor(Vector4f(0.5f, 0.5f, 0.5f, 1f))
        dddOut.clearDepth(1f)
        onFrame(deltaTime)
        dddOut.blitColorOnto(uiOut)

        ui.update()
        uiOut.blitColorOnto(window.framebuffer)
        
        window.endFrame()
        Thread.sleep(15)
    }
    window.dispose()
    resLoader.submit(ResourceLoader.stop)
}