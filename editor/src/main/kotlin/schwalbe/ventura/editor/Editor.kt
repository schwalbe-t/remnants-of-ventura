
package schwalbe.ventura.editor

import org.joml.Matrix4f
import schwalbe.ventura.client.*
import schwalbe.ventura.client.game.CameraController
import schwalbe.ventura.engine.*
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.engine.ui.UiNavigator
import kotlin.concurrent.thread
import org.joml.Vector3f
import org.joml.Vector4f
import schwalbe.ventura.client.game.ChunkLoader
import schwalbe.ventura.engine.input.Key
import schwalbe.ventura.engine.input.isPressed

class Editor {

    companion object {
        const val BOOSTED_SPEED_MULTIPLIER: Float = 2f
    }


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

    var deltaTime: Float = 0f

    val renderer = Renderer(
        this.out3d,
        camera = Camera(far = 500f)
    )


    var position = Vector3f()
    val cameraMode = CameraController.Mode(
        lookAt = { _ -> this.position },
        fovDegrees = 30f
    )
    val camController = CameraController(
        this.cameraMode, minDist = 5f, maxDist = 100f
    )

}

fun Editor.loadResources() {
    thread { this.resLoader.loadQueuedRawLoop() }
}

fun Editor.gameloop() {
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
        this.nav.currentOrNull?.render()
        this.out3d.blitColorOnto(this.out2d)
        this.nav.update()
        this.out2d.blitColorOnto(windowBuff)

        this.window.endFrame()
    }
}

private fun Editor.move() {
    val velocity = Vector3f()
    if (Key.A.isPressed) { velocity.x -= 1f }
    if (Key.D.isPressed) { velocity.x += 1f }
    if (Key.W.isPressed) { velocity.z -= 1f }
    if (Key.S.isPressed) { velocity.z += 1f }
    if (velocity.lengthSquared() == 0f) { return }
    velocity.normalize()
        .mul(this.camController.userDistance)
        .mul(this.deltaTime)
    if (Key.LEFT_CONTROL.isPressed) {
        velocity.mul(Editor.BOOSTED_SPEED_MULTIPLIER)
    }
    this.position.add(velocity)
}

fun Editor.update() {
    this.move()
    this.camController.update(
        this.renderer.camera, this.renderer, captureInput = true
    )
}

fun Editor.render() {
    this.renderer.update(this.position)
    this.renderer.forEachPass { pass ->
        val model = ChunkLoader.objectModels[0]()
        pass.renderGeometry(model, null, listOf(Matrix4f()))
    }
}

fun Editor.dispose() {
    this.window.dispose()
    this.resLoader.submit(ResourceLoader.stop)
}
