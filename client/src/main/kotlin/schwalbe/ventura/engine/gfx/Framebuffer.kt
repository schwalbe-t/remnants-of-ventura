
package schwalbe.ventura.engine.gfx

import schwalbe.ventura.engine.UsageAfterDisposalException
import schwalbe.ventura.engine.Disposable

import org.lwjgl.opengl.GL33.*
import org.joml.*

abstract class ConstFramebuffer {

    abstract val width: Int
    abstract val height: Int

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
    
    internal abstract fun bind(glTarget: Int = GL_FRAMEBUFFER)

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

    fun attachColor(newColor: Texture?): Framebuffer {
        glBindFramebuffer(GL_FRAMEBUFFER, this.getFboId())
        val target: Int = newColor?.glTarget ?: GL_TEXTURE_2D
        glFramebufferTexture2D(
            GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, target,
            newColor?.getTexId() ?: 0,
            0
        )
        val buff = if (newColor == null) { GL_NONE }
            else { GL_COLOR_ATTACHMENT0 }
        glDrawBuffer(buff)
        glReadBuffer(buff)
        this.color = newColor
        this.computeProperties()
        return this
    }

    fun attachDepth(newDepth: Texture?): Framebuffer {
        glBindFramebuffer(GL_FRAMEBUFFER, this.getFboId())
        val target: Int = newDepth?.glTarget ?: GL_TEXTURE_2D
        glFramebufferTexture2D(
            GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, target,
            newDepth?.getTexId() ?: 0,
            0
        )
        this.depth = newDepth
        this.computeProperties()
        return this
    }
    
    fun resize(newWidth: Int, newHeight: Int) {
        val resize: Boolean = this.width != newWidth || this.height != newHeight
        if (!resize) { return }
        val oldColor: Texture? = this.color
        val oldDepth: Texture? = this.depth
        this.attachColor(null)
        this.attachDepth(null)
        if (oldColor != null) {
            this.attachColor(Texture(
                newWidth, newHeight, oldColor.filter, oldColor.format,
                oldColor.samples
            ))
            oldColor.dispose()
        }
        if (oldDepth != null) {
            this.attachDepth(Texture(
                newWidth, newHeight, oldDepth.filter, oldDepth.format,
                oldDepth.samples
            ))
            oldDepth.dispose()
        }
    }

    private fun blitOnto(
        target: ConstFramebuffer,
        getTexture: (Framebuffer) -> Texture?, glComp: Int
    ) {
        val thisTex: Texture? = getTexture(this)
        require(thisTex != null)
        if (target is Framebuffer) {
            val targetTex: Texture? = getTexture(target)
            require(targetTex != null)
            require(thisTex.format.glIFmt == targetTex.format.glIFmt)
            if (thisTex.samples == 1) { require(targetTex.samples == 1) }
        }
        if (thisTex.samples > 1) {
            require(this.width == target.width && this.height == target.height)
        }
        this.bind(GL_READ_FRAMEBUFFER)
        target.bind(GL_DRAW_FRAMEBUFFER)
        glBlitFramebuffer(
            0, 0, this.width, this.height,
            0, 0, target.width, target.height,
            glComp, GL_NEAREST
        )
    }

    fun blitColorOnto(target: ConstFramebuffer)
        = this.blitOnto(target, Framebuffer::color, GL_COLOR_BUFFER_BIT)

    fun blitDepthOnto(target: ConstFramebuffer)
        = this.blitOnto(target, Framebuffer::depth, GL_DEPTH_BUFFER_BIT)

    override fun bind(glTarget: Int) {
        if (!this.complete) {
            throw IllegalStateException("Framebuffer is incomplete")
        }
        glBindFramebuffer(glTarget, this.getFboId())
        glViewport(0, 0, this.width, this.height)
    }

    override fun dispose() {
        val oldId: Int = this.fboId ?: return
        if (this.color != null) { this.attachColor(null) }
        if (this.depth != null) { this.attachDepth(null) }
        glDeleteFramebuffers(oldId)
        this.fboId = null
    }

}