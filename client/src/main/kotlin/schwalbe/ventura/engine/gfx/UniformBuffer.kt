
package schwalbe.ventura.engine.gfx

import schwalbe.ventura.engine.Disposable
import schwalbe.ventura.engine.UsageAfterDisposalException
import org.lwjgl.opengl.GL33.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class UniformBuffer(val writeHint: BufferWriteFreq) : Disposable {
    
    companion object
    
    
    var bufferId: Int? = glGenBuffers()
        private set
    var lastSize: Int? = null
        private set
        
    fun getBufferId(): Int = this.bufferId
        ?: throw UsageAfterDisposalException()
        
    fun write(data: ByteBuffer): UniformBuffer {
        this.bind()
        val lastSize: Int? = this.lastSize
        if (lastSize == null || data.remaining() > lastSize) {
            this.lastSize = data.remaining()
            glBufferData(GL_UNIFORM_BUFFER, data, this.writeHint.glValue)
        } else {
            glBufferSubData(GL_UNIFORM_BUFFER, 0, data)
        }
        return this
    }
    
    fun write(data: FloatArray): UniformBuffer {
        val buffer: ByteBuffer = ByteBuffer
            .allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
        buffer.asFloatBuffer().put(data)
        return this.write(buffer)
    }

    internal fun bind() {
        glBindBuffer(GL_UNIFORM_BUFFER, this.getBufferId())
    }
        
    override fun dispose() {
        val id: Int = this.bufferId ?: return
        glDeleteBuffers(id)
        this.bufferId = null
    }
    
}
