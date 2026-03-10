
package schwalbe.ventura.editor

import schwalbe.ventura.client.*
import schwalbe.ventura.client.game.CameraController
import schwalbe.ventura.engine.*
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.engine.ui.UiNavigator
import kotlin.concurrent.thread
import org.joml.Vector3f
import org.joml.Vector4f

class Editor {

    val resLoader = ResourceLoader()

    val window = Window(
        name = "Ventura World Editor",
        sizeFactor = 0.9f,
        iconPath = "res/icon.png"
    )

    init {
        this.window.setVsyncEnabled(true)
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

    val nav = UiNavigator<EditorScreen>(this.out2d, this.window.inputEvents)

    val renderer = Renderer(this.out3d)


    var position = Vector3f()
    val cameraMode = CameraController.Mode(
        lookAt = { _ -> this.position },
        fovDegrees = 30f
    )
    val camController = CameraController(this.cameraMode, 5f, 50f)

}

fun Editor.loadResources() {
    thread { this.resLoader.loadQueuedRawLoop() }
}

fun Editor.gameloop() {
    while (!this.window.shouldClose()) {
        this.window.beginFrame()
        this.resLoader.loadQueuedFully()

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

fun Editor.update() {
    this.camController.update(
        this.renderer.camera, this.renderer, captureInput = true
    )
}

fun Editor.render() {
    this.renderer.update(this.position)
    this.renderer.forEachPass { pass ->

    }
}

fun Editor.dispose() {
    this.window.dispose()
    this.resLoader.submit(ResourceLoader.stop)
}
