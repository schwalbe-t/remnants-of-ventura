
package schwalbe.ventura.engine.gfx

import schwalbe.ventura.engine.UsageAfterDisposalException
import schwalbe.ventura.engine.Disposable

import org.lwjgl.opengl.GL33.*
import org.joml.*

interface ConstFramebuffer : Bindable {

    companion object {
        internal val bound = BindingManager<ConstFramebuffer>()
    }

    val width: Int
    val height: Int

    fun clearColor(color: Vector4fc) {
        this.bind()
        glClearColor(color.x(), color.y(), color.z(), color.w())
        glClear(GL_COLOR_BUFFER_BIT)
    }

    fun clearDepth(depth: Float) {
        this.bind()
        glClearDepth(depth.toDouble())
        glClear(GL_DEPTH_BUFFER_BIT)
    }

}

class Framebuffer : ConstFramebuffer, Disposable {

    var fboId: Int? = null
        private set

    var color: Texture? = null
        private set
    var depth: Texture? = null
        private set

    var complete: Boolean = false
        private set
    override var width: Int = 0
        private set
    override var height: Int = 0
        private set

    fun getFboId(): Int
        = this.fboId ?: throw UsageAfterDisposalException()

    constructor() {
        this.fboId = glGenFramebuffers()
        ConstFramebuffer.bound.invalidateAll()
        glBindFramebuffer(GL_FRAMEBUFFER, this.getFboId())
        glDrawBuffer(GL_NONE)
        glReadBuffer(GL_NONE)
    }

    private fun assertMatchingDimensions(base: Texture, test: Texture?) {
        if (test == null) { return }
        val matching: Boolean = base.width == test.width
            && base.height == test.height
        if (matching) { return }
        throw IllegalStateException(
            "Framebuffer color and depth dimensions don't match"
        )
    }

    private fun computeProperties() {
        val base: Texture? = this.color ?: this.depth
        if (base === null) {
            this.complete = false
            this.width = 0
            this.height = 0
            return
        }
        this.complete = true
        this.width = base.width
        this.height = base.height
        this.assertMatchingDimensions(base, this.color)
        this.assertMatchingDimensions(base, this.depth)
    }

    fun attachColor(newColor: Texture?) {
        ConstFramebuffer.bound.invalidateAll()
        glBindFramebuffer(GL_FRAMEBUFFER, this.getFboId())
        glFramebufferTexture2D(
            GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D,
            newColor?.getTexId() ?: 0,
            0
        )
        val buff = if (newColor == null) { GL_NONE }
            else { GL_COLOR_ATTACHMENT0 }
        glDrawBuffer(buff)
        glReadBuffer(buff)
        this.color = newColor
        this.computeProperties()
    }

    fun attachDepth(newDepth: Texture?) {
        ConstFramebuffer.bound.invalidateAll()
        glBindFramebuffer(GL_FRAMEBUFFER, this.getFboId())
        glFramebufferTexture2D(
            GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D,
            newDepth?.getTexId() ?: 0,
            0
        )
        this.depth = newDepth
        this.computeProperties()
    }

    override fun bind() {
        if (!this.complete) {
            throw IllegalStateException("Framebuffer is incomplete")
        }
        glBindFramebuffer(GL_FRAMEBUFFER, this.getFboId())
        glViewport(0, 0, this.width, this.height)
    }

    override fun dispose() {
        val oldId: Int? = this.fboId
        if (oldId == null) { return }
        if (this.color != null) { this.attachColor(null) }
        if (this.depth != null) { this.attachDepth(null) }
        glDeleteFramebuffers(oldId)
        this.fboId = null
    }

}