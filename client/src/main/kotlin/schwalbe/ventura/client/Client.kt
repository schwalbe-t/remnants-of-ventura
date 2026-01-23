
package schwalbe.ventura.client

import org.joml.Vector4f
import schwalbe.ventura.client.screens.submitScreenResources
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.engine.*
import kotlin.concurrent.thread

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

    val nav = UiNavigator(this.out2d, this.window.inputEvents)

    var deltaTime: Float = 0f
    var onFrame: () -> Unit = {}

}

fun Client.loadResources() {
    thread { this.resLoader.loadQueuedRawLoop() }

    loadUiResources(this.resLoader)
    submitScreenResources(this.resLoader)
    this.resLoader.submit(localized)
}

fun Client.gameloop() {
    var lastFrameTime: Long = System.nanoTime()
    while (!this.window.shouldClose()) {
        this.window.beginFrame()
        this.resLoader.loadQueuedFully()

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
        this.onFrame()
        this.out3d.blitColorOnto(this.out2d)

        this.nav.update()
        this.out2d.blitColorOnto(windowBuff)

        this.window.endFrame()
    }
}

fun Client.dispose() {
    this.window.dispose()
    this.resLoader.submit(ResourceLoader.stop)
}