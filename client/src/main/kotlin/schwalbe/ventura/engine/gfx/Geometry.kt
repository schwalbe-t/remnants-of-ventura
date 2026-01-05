
package schwalbe.ventura.engine.gfx

import schwalbe.ventura.engine.Disposable
import schwalbe.ventura.engine.UsageAfterDisposalException

import org.lwjgl.opengl.GL33.*
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import java.nio.IntBuffer
import java.nio.ByteOrder

fun Iterable<Geometry.Attribute>.computeStride(): Int
    = this.sumOf(Geometry.Attribute::numBytes)

private fun configureVertexArrayObject(layout: List<Geometry.Attribute>) {
    val stride: Int = layout.computeStride()
    var byteOffset = 0L
    for ((attribI, attrib) in layout.withIndex()) {
        glEnableVertexAttribArray(attribI)
        val compSize: Int = attrib.compType.numBytes
        check(byteOffset % compSize == 0L) {
            "Attribute [$attribI] needs a vertex offset that is a multiple of" +
                " $compSize byte(s), but has an offset of $byteOffset byte(s)"
        }
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

private fun createBindGeometryBuffers(): Triple<Int, Int, Int> {
    val vaoId: Int = glGenVertexArrays()
    glBindVertexArray(vaoId)
    val vboId: Int = glGenBuffers()
    glBindBuffer(GL_ARRAY_BUFFER, vboId)
    val eboId: Int = glGenBuffers()
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId)
    return Triple(vaoId, vboId, eboId)
}

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
    
    companion object
    
    
    var indexCount: Int
        private set
    var vaoId: Int? = null
        private set
    var vboId: Int? = null
        private set
    var eboId: Int? = null
        private set
    val indexType: Int
    
    constructor(layout: List<Attribute>, vbo: ByteBuffer, ebo: ShortBuffer) {
        this.indexCount = ebo.remaining()
        val (vaoId, vboId, eboId) = createBindGeometryBuffers()
        glBufferData(GL_ARRAY_BUFFER, vbo, BufferWriteFreq.ONCE.glValue)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ebo, BufferWriteFreq.ONCE.glValue)
        configureVertexArrayObject(layout)
        this.vaoId = vaoId
        this.vboId = vboId
        this.eboId = eboId
        this.indexType = GL_UNSIGNED_SHORT
    }
    
    constructor(layout: List<Attribute>, vbo: ByteBuffer, ebo: IntBuffer) {
        this.indexCount = ebo.remaining()
        val (vaoId, vboId, eboId) = createBindGeometryBuffers()
        glBufferData(GL_ARRAY_BUFFER, vbo, BufferWriteFreq.ONCE.glValue)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ebo, BufferWriteFreq.ONCE.glValue)
        configureVertexArrayObject(layout)
        this.vaoId = vaoId
        this.vboId = vboId
        this.eboId = eboId
        this.indexType = GL_UNSIGNED_INT
    }
    
    internal fun bind() {
        val vaoId: Int = this.vaoId ?: throw UsageAfterDisposalException()
        glBindVertexArray(vaoId)
    }
    
    fun render(
        shader: Shader<*, *>, framebuffer: ConstFramebuffer,
        instanceCount: Int = 1,
        faceCulling: FaceCulling = FaceCulling.DISABLED,
        depthTesting: DepthTesting = DepthTesting.ENABLED
    ) {
        faceCulling.glApply()
        depthTesting.glApply()
        framebuffer.bind()
        shader.bind()
        this.bind()
        if (instanceCount > 1) {
            glDrawElementsInstanced(
                GL_TRIANGLES, this.indexCount, this.indexType, 0,
                instanceCount
            )
        } else if (instanceCount == 1) {
            glDrawElements(
                GL_TRIANGLES, this.indexCount, this.indexType, 0
            )
        }
    }
    
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
    }
    
}

fun Geometry.Companion.byte(n: Int)
    = Geometry.Attribute(n, Geometry.Type.BYTE)
fun Geometry.Companion.ubyte(n: Int)
    = Geometry.Attribute(n, Geometry.Type.UBYTE)
fun Geometry.Companion.short(n: Int)
    = Geometry.Attribute(n, Geometry.Type.SHORT)
fun Geometry.Companion.ushort(n: Int)
    = Geometry.Attribute(n, Geometry.Type.USHORT)
fun Geometry.Companion.int(n: Int)
    = Geometry.Attribute(n, Geometry.Type.INT)
fun Geometry.Companion.uint(n: Int)
    = Geometry.Attribute(n, Geometry.Type.UINT)
fun Geometry.Companion.float(n: Int)
    = Geometry.Attribute(n, Geometry.Type.FLOAT)

fun Geometry.Companion.fromFloatArray(
    layout: List<Geometry.Attribute>, vbo: FloatArray, ebo: ShortArray
): Geometry {
    val vertexBuffer: ByteBuffer = ByteBuffer
        .allocateDirect(vbo.size * 4)
        .order(ByteOrder.nativeOrder())
    vertexBuffer.asFloatBuffer().put(vbo)
    val elementBuffer: ShortBuffer = ByteBuffer
        .allocateDirect(ebo.size * 2)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()
        .put(ebo)
        .flip()
    return Geometry(layout, vertexBuffer, elementBuffer)
}
