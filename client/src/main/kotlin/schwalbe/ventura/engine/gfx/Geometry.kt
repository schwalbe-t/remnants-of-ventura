
package schwalbe.ventura.engine.gfx

import schwalbe.ventura.engine.Disposable
import schwalbe.ventura.engine.UsageAfterDisposalException

import org.lwjgl.opengl.GL33.*
import java.nio.ByteBuffer
import java.nio.ShortBuffer

class Geometry : Disposable {
    
    enum class Type(val numBytes: Int, val glType: Int, val isInt: Boolean) {
        BYTE    (1, GL_BYTE,            true    ),
        UBYTE   (1, GL_UNSIGNED_BYTE,   true    ),
        SHORT   (2, GL_SHORT,           true    ),
        USHORT  (2, GL_UNSIGNED_SHORT,  true    ),
        INT     (4, GL_INT,             true    ),
        UINT    (4, GL_UNSIGNED_INT,    true    ),
        FLOAT   (4, GL_FLOAT,           false   )
    }
    
    data class Attribute(val numComps: Int, val compType: Type) {
        val numBytes: Int
            get() = this.numComps * this.compType.numBytes
    }
    
    companion object {
        internal val bound = BindingManager<Geometry>(Geometry::bind)
    }
    
    
    var vaoId: Int? = null
        private set
    var vboId: Int? = null
        private set
    var eboId: Int? = null
        private set
    var indexCount: Int
        private set
    
    constructor(layout: List<Attribute>, vbo: ByteBuffer, ebo: ShortBuffer) {
        this.indexCount = ebo.remaining()
        val vaoId: Int = glGenVertexArrays()
        this.vaoId = vaoId
        glBindVertexArray(vaoId)
        val vboId: Int = glGenBuffers()
        this.vboId = vboId
        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        glBufferData(GL_ARRAY_BUFFER, vbo, GL_STATIC_DRAW)
        val eboId: Int = glGenBuffers()
        this.eboId = eboId
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ebo, GL_STATIC_DRAW)
        val stride: Int = layout.sumOf(Attribute::numBytes)
        var byteOffset = 0L
        for ((attribI, attrib) in layout.withIndex()) {
            glEnableVertexAttribArray(attribI)
            if (attrib.compType.isInt) {
                glVertexAttribIPointer(
                    attribI, attrib.numComps, attrib.compType.glType, stride,
                    byteOffset
                )
            } else {
                glVertexAttribPointer(
                    attribI, attrib.numComps, attrib.compType.glType, false,
                    stride, byteOffset
                )
            }
            byteOffset += attrib.numBytes
        }
    }
    
    private fun bind() {
        val vaoId: Int = this.vaoId ?: throw UsageAfterDisposalException()
        glBindVertexArray(vaoId)
    }
    
    // TODO! fun render(...) { ... }
    
    override fun dispose() {
        val oldVaoId: Int? = this.vaoId
        if (oldVaoId != null) {
            glDeleteVertexArrays(oldVaoId)
            this.vaoId = null
        }
        val oldVboId: Int? = this.vboId
        if (oldVboId != null) {
            glDeleteBuffers(oldVboId)
            this.vboId = null
        }
        val oldEboId: Int? = this.eboId
        if (oldEboId != null) {
            glDeleteBuffers(oldEboId)
            this.eboId = null
        }
        this.indexCount = 0
        Geometry.bound.invalidate(this)
    }
    
}