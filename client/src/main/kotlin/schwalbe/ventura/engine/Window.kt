
package schwalbe.ventura.engine

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryStack
import schwalbe.ventura.engine.gfx.ConstFramebuffer
import schwalbe.ventura.engine.gfx.DepthTesting
import schwalbe.ventura.engine.gfx.FaceCulling

private class WindowFramebuffer(val window: Window) : ConstFramebuffer() {    
    override val width: Int
        get() = this.window.width
    override val height: Int
        get() = this.window.height
    
    override fun bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        glViewport(0, 0, this.width, this.height)
    }
}

class Window : Disposable {

    private var windowId: Long? = null
    var width: Int = 0
        private set
    var height: Int = 0
        private set
        
    constructor(name: String) {
        check(glfwInit()) { "Failed to initialize GLFW" }
        val monitor: Long = glfwGetPrimaryMonitor()
        val vidMode: GLFWVidMode = glfwGetVideoMode(monitor)
            ?: throw IllegalStateException("Failed to get primary monitor")
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_RED_BITS, vidMode.redBits());
        glfwWindowHint(GLFW_GREEN_BITS, vidMode.greenBits());
        glfwWindowHint(GLFW_BLUE_BITS, vidMode.blueBits());
        glfwWindowHint(GLFW_REFRESH_RATE, vidMode.refreshRate());
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE)
        val windowId: Long = glfwCreateWindow(
            vidMode.width(), vidMode.height(), name, monitor, NULL
        )
        this.windowId = windowId
        glfwMakeContextCurrent(windowId)
        this.initGraphics()
    }
    
    private fun initGraphics() {
        GL.createCapabilities()
        DepthTesting.bound.bindEager(DepthTesting.ENABLED)
        FaceCulling.bound.bindEager(FaceCulling.DISABLED)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    }
    
    fun getWindowId(): Long = this.windowId
        ?: throw UsageAfterDisposalException()
    
    fun setVsyncEnabled(enabled: Boolean = true) {
        glfwSwapInterval(if (enabled) 1 else 0)
    }
    
    val framebuffer: ConstFramebuffer = WindowFramebuffer(this)
    
    fun shouldClose(): Boolean = glfwWindowShouldClose(this.getWindowId())
    
    fun beginFrame() {
        glfwPollEvents()
        MemoryStack.stackPush().use { stack ->
            val widthPtr = stack.mallocInt(1)
            val heightPtr = stack.mallocInt(1)
            glfwGetFramebufferSize(this.getWindowId(), widthPtr, heightPtr)
            val newWidth: Int = widthPtr.get(0)
            val newHeight: Int = heightPtr.get(0)
            if (newWidth == this.width && newHeight == this.height) { return }
            ConstFramebuffer.bound.invalidate(this.framebuffer)
            this.width = newWidth
            this.height = newHeight
        }
    }
    
    fun endFrame() {
        glfwSwapBuffers(this.getWindowId())
    }
    
    override fun dispose() {
        val windowId: Long = this.windowId ?: return
        glfwDestroyWindow(windowId)
        glfwTerminate()
        this.width = 0
        this.height = 0
    }
    
}