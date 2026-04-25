
package schwalbe.ventura.client

import schwalbe.ventura.engine.*
import schwalbe.ventura.engine.fromCallback
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.engine.input.isPressed
import schwalbe.ventura.engine.ui.UiNavigator
import schwalbe.ventura.engine.ui.UiScreen
import org.joml.Vector4f
import kotlin.concurrent.thread

interface ApplicationScreen<S : UiScreen<S>> {
    val render: () -> Unit
}

open class Application<S>(
    val window: Window,
    val shouldReloadResources: () -> Boolean = { false }
)
where S : UiScreen<S>, S : ApplicationScreen<S> {

    val resLoader = ResourceLoader()

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

    val nav = UiNavigator<S>(this.out2d, this.window.inputEvents)

    var isSuspended: Boolean = false
    var deltaTime: Float = 0f


    open fun beforeRender() {}

    open fun dispose() {
        this.window.dispose()
        this.resLoader.submit(ResourceLoader.stop)
        this.resLoader.disposeAll()
    }

}

fun Application<*>.loadResources() {
    thread { this.resLoader.loadQueuedRawLoop() }
}

fun Application<*>.reloadAllResources() {
    this.isSuspended = true
    this.resLoader.resubmitAll()
    this.resLoader.submit(Resource.fromCallback {
        this.isSuspended = false
        println("Reloaded all resources")
    })
}

fun Application<*>.gameloop() {
    var lastFrameTime: Long = System.nanoTime()
    while (!this.window.shouldClose()) {
        if (this.isSuspended) {
            this.resLoader.loadQueuedFully()
            continue
        }

        this.window.beginFrame()

        this.beforeRender()

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

        if (this.shouldReloadResources()) {
            this.reloadAllResources()
        }

        this.window.endFrame()

        this.resLoader.loadQueuedFully()
    }
}
