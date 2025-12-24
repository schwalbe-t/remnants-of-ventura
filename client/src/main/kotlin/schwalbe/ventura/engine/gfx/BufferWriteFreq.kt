
package schwalbe.ventura.engine.gfx

import org.lwjgl.opengl.GL33.*

enum class BufferWriteFreq(val glValue: Int) {
    ONCE        (GL_STATIC_DRAW ),
    SOMETIMES   (GL_DYNAMIC_DRAW),
    EVERY_FRAME (GL_STREAM_DRAW )
}
