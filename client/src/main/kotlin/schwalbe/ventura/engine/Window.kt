
package schwalbe.ventura.engine

import schwalbe.ventura.engine.gfx.ConstFramebuffer
import schwalbe.ventura.engine.gfx.DepthTesting
import schwalbe.ventura.engine.gfx.FaceCulling
import schwalbe.ventura.engine.input.InputEventQueue
import schwalbe.ventura.engine.input.Keyboard
import schwalbe.ventura.engine.input.Mouse
import schwalbe.ventura.engine.input.Cursor
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import org.lwjgl.openal.ALC10.*
import org.lwjgl.openal.ALC
import org.lwjgl.openal.AL
import org.lwjgl.stb.STBImage.stbi_load
import org.lwjgl.stb.STBImage.stbi_set_flip_vertically_on_load
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer
import kotlin.math.roundToInt

private class WindowFramebuffer(val window: Window) : ConstFramebuffer() {    
    override val width: Int
        get() = this.window.width
    override val height: Int
        get() = this.window.height
    
    override fun bind(glTarget: Int) {
        glBindFramebuffer(glTarget, 0)
        glViewport(0, 0, this.width, this.height)
    }
}

class Window(
    name: String,
    val sizeFactor: Float,
    iconPath: String? = null
) : Disposable {

    private var windowId: Long? = null
    var width: Int = 0
        private set
    var height: Int = 0
        private set
        
    val inputEvents: InputEventQueue

    init {
        check(glfwInit()) { "Failed to initialize GLFW" }
        val monitor: Long = glfwGetPrimaryMonitor()
        val vidMode: GLFWVidMode = glfwGetVideoMode(monitor)
            ?: throw IllegalStateException("Failed to get primary monitor")
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
//        glfwWindowHint(GLFW_RED_BITS, vidMode.redBits())
//        glfwWindowHint(GLFW_GREEN_BITS, vidMode.greenBits())
//        glfwWindowHint(GLFW_BLUE_BITS, vidMode.blueBits())
//        glfwWindowHint(GLFW_REFRESH_RATE, vidMode.refreshRate())
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE)
        val windowId: Long = glfwCreateWindow(500, 500, name, NULL, NULL)
        this.windowId = windowId
        glfwMakeContextCurrent(windowId)
        this.initGraphics()
        this.initAudio()
        this.inputEvents = InputEventQueue(windowId)
        if (iconPath != null) {
            this.loadIcon(iconPath)
        }
        this.setFullscreenEnabled(false)
    }
    
    private fun initGraphics() {
        GL.createCapabilities()
        DepthTesting.ENABLED.glApply()
        FaceCulling.DISABLED.glApply()
        glEnable(GL_BLEND)
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun initAudio() {
        val device = alcOpenDevice(null as ByteBuffer?)
        val context = alcCreateContext(device, intArrayOf(0))
        alcMakeContextCurrent(context)
        AL.createCapabilities(ALC.createCapabilities(device))
    }

    private fun loadIcon(path: String) = MemoryStack.stackPush().use { stack ->
        val widthPtr = stack.mallocInt(1)
        val heightPtr = stack.mallocInt(1)
        val channelsPtr = stack.mallocInt(1)
        stbi_set_flip_vertically_on_load(false)
        val decoded: ByteBuffer
            = stbi_load(path, widthPtr, heightPtr, channelsPtr, 4) ?: return@use
        val images = GLFWImage.calloc(1, stack)
        images[0].run {
            width(widthPtr.get(0))
            height(heightPtr.get(0))
            pixels(decoded)
        }
        glfwSetWindowIcon(this.getWindowId(), images)
    }
    
    fun getWindowId(): Long = this.windowId
        ?: throw UsageAfterDisposalException()

    fun setVisible(visible: Boolean = true): Unit = when (visible) {
        true    -> glfwShowWindow(this.getWindowId())
        false   -> glfwHideWindow(this.getWindowId())
    }

    fun setVsyncEnabled(enabled: Boolean = true) {
        glfwSwapInterval(if (enabled) 1 else 0)
    }

    fun setFullscreenEnabled(enabled: Boolean = true) {
        val monitor: Long = glfwGetPrimaryMonitor()
        val vidMode: GLFWVidMode = glfwGetVideoMode(monitor)
            ?: throw IllegalStateException("Failed to get primary monitor")
        val windowId: Long = this.getWindowId()
        if (enabled) {
            glfwSetWindowMonitor(
                windowId, monitor, 0, 0,
                vidMode.width(), vidMode.height(), vidMode.refreshRate()
            )
        } else {
            val width: Int = (this.sizeFactor * vidMode.width()).roundToInt()
            val height: Int = (this.sizeFactor * vidMode.height()).roundToInt()
            val windowX: Int = (vidMode.width() - width) / 2
            val windowY: Int = (vidMode.height() - height) / 2
            glfwSetWindowMonitor(
                windowId, NULL, windowX, windowY, width, height, -1
            )
        }
    }
    
    val framebuffer: ConstFramebuffer = WindowFramebuffer(this)
    
    fun shouldClose(): Boolean = glfwWindowShouldClose(this.getWindowId())
    
    fun beginFrame() {
        glfwPollEvents()
        Mouse.cursor = Cursor.ARROW
        MemoryStack.stackPush().use { stack ->
            val widthPtr = stack.mallocInt(1)
            val heightPtr = stack.mallocInt(1)
            glfwGetFramebufferSize(this.getWindowId(), widthPtr, heightPtr)
            val newWidth: Int = widthPtr.get(0)
            val newHeight: Int = heightPtr.get(0)
            if (newWidth == this.width && newHeight == this.height) { return }
            this.width = newWidth
            this.height = newHeight
        }
    }
    
    fun flushInputEvents() {
        this.inputEvents.remaining().forEach {
            Keyboard.handleEvent(it)
            Mouse.handleEvent(it)
        }
        this.inputEvents.clear()
    }
    
    private var prevCursor: Cursor = Cursor.ARROW
    private val cursors: MutableMap<Int, Long> = mutableMapOf()
    
    fun endFrame() {
        val windowId: Long = this.getWindowId()
        if (Mouse.cursor != this.prevCursor) {
            val cursorConst: Int = Mouse.cursor.glfwCursorConst
            val cursorId: Long = this.cursors
                .getOrPut(cursorConst) { glfwCreateStandardCursor(cursorConst) }
            glfwSetCursor(windowId, cursorId)
            this.prevCursor = Mouse.cursor
        }
        glfwSwapBuffers(windowId)
        Keyboard.onFrameEnd()
        Mouse.onFrameEnd()
    }

    fun close() {
        glfwSetWindowShouldClose(this.getWindowId(), true)
    }
    
    override fun dispose() {
        val windowId: Long = this.windowId ?: return
        glfwDestroyWindow(windowId)
        glfwTerminate()
        this.width = 0
        this.height = 0
    }
    
}