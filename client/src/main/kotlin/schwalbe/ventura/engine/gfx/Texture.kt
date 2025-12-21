
package schwalbe.ventura.engine.gfx

import schwalbe.ventura.engine.UsageAfterDisposalException
import schwalbe.ventura.engine.Disposable

import org.lwjgl.opengl.GL33.*
import java.nio.ByteBuffer

class Texture : Disposable {
    
    enum class Format(val glFmt: Int, val glIFmt: Int, val glChType: Int) {
        R8      (GL_RED,    GL_R8,      GL_UNSIGNED_BYTE),
        RG8     (GL_RG,     GL_RG8,     GL_UNSIGNED_BYTE),
        RGB8    (GL_RGB,    GL_RGB8,    GL_UNSIGNED_BYTE),
        RGBA8   (GL_RGBA,   GL_RGBA8,   GL_UNSIGNED_BYTE),
        
        DEPTH16(
            GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT16,   GL_UNSIGNED_SHORT
        ),
        DEPTH24(
            GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT24,   GL_UNSIGNED_INT
        ),
        DEPTH32(
            GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT32F,  GL_FLOAT
        )
    }
    
    enum class Filter(val glValue: Int) {
        LINEAR(GL_LINEAR),
        NEAREST(GL_NEAREST)
    }
    
    companion object {
        internal val bound = BindingManager<Texture>(Texture::bind)
    }
    
    var texId: Int? = null
        private set

    val width: Int
    val height: Int

    constructor(
        width: Int, height: Int, filter: Filter,
        format: Format, data: ByteBuffer? = null
    ) {
        require(width >= 1 && height >= 1)
        val id: Int = glGenTextures()
        this.texId = id
        this.width = width
        this.height = height
        Texture.bound.bindEager(this)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter.glValue)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter.glValue)
        glTexImage2D(
            GL_TEXTURE_2D, 0, format.glIFmt, width, height, 0,
            format.glFmt, format.glChType, data
        )
    }
    
    fun getTexId(): Int
        = this.texId ?: throw UsageAfterDisposalException()

    private fun bind() {
        glBindTexture(GL_TEXTURE_2D, this.getTexId())
    }

    override fun dispose() {
        val oldId: Int = this.texId ?: return
        glDeleteTextures(oldId)
        Texture.bound.invalidate(this)
    }

}