
package schwalbe.ventura.client

import schwalbe.ventura.client.screens.GameScreen
import schwalbe.ventura.client.game.World
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.engine.*
import kotlin.concurrent.thread
import org.joml.Vector4f

class Client {

    val config: Config = Config.read()

    val resLoader = ResourceLoader()

    val window = Window("Remnants of Ventura", fullscreen = false)

    init {
        this.window.setVsyncEnabled(enabled = true)
    }

    val out3d = Framebuffer()
        .attachColor(Texture(
            16, 16, Texture.Filter.NEAREST, Texture.Format.RGBA8, samples = 4
        ))
        .attachDepth(Texture(
            16, 16, Texture.Filter.NEAREST, Texture.Format.DEPTH24, samples = 4
        ))
    val out2d = Framebuffer()
        .attachColor(Texture(
            16, 16, Texture.Filter.NEAREST, Texture.Format.RGBA8
        ))

    val nav = UiNavigator<GameScreen>(this.out2d, this.window.inputEvents)

    var deltaTime: Float = 0f

    val network = NetworkClient()

    val renderer = Renderer(this.out3d)
    var username: String = ""
    var world: World? = World()

}

fun Client.loadResources() {
    thread { this.resLoader.loadQueuedRawLoop() }
}

fun Client.gameloop() {
    var lastFrameTime: Long = System.nanoTime()
    while (!this.window.shouldClose()) {
        this.window.beginFrame()
        this.resLoader.loadQueuedFully()

        this.nav.currentOrNull?.networkState()
        this.network.handlePackets(this.nav.currentOrNull?.packets)

        val now: Long = System.nanoTime()
        val deltaTimeNanos: Long = now - lastFrameTime
        this.deltaTime = (deltaTimeNanos.toDouble() * 0.000_000_001)
            .toFloat()
        lastFrameTime = now

        val windowBuff: ConstFramebuffer = this.window.framebuffer
        this.out3d.resize(windowBuff.width, windowBuff.height)
        this.out2d.resize(windowBuff.width, windowBuff.height)

        this.nav.captureInput()
        this.window.flushInputEvents()

        this.out3d.clearColor(Vector4f(0.5f, 0.5f, 0.5f, 1f))
        this.out3d.clearDepth(1f)
        this.nav.currentOrNull?.render()
        this.out3d.blitColorOnto(this.out2d)

        this.nav.update()
        this.out2d.blitColorOnto(windowBuff)

        this.window.endFrame()
    }
}

fun Client.dispose() {
    this.window.dispose()
    this.resLoader.submit(ResourceLoader.stop)
    this.network.dispose()
}