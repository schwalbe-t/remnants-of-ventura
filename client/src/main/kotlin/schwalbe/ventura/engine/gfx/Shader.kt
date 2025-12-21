
package schwalbe.ventura.engine.gfx

import schwalbe.ventura.engine.Disposable

import org.lwjgl.opengl.GL33.*

private fun compileShader(
    src: String, path: String, glType: Int, dispType: String
): Int {
    val shaderId: Int = glCreateShader(glType)
    glShaderSource(shaderId, src)
    glCompileShader(shaderId)
    check(glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_TRUE) {
        val infoLog: String = glGetShaderInfoLog(shaderId)
        glDeleteShader(shaderId)
        "Compilation of $dispType shader '$path' failed:\n\n$infoLog"
    }
    return shaderId
}

class Shader : Disposable {
    
    var programId: Int? = null
        private set
    
    constructor(
        vertSrc: String, fragSrc: String,
        vertPath: String = "<unknown>", fragPath: String = "<unknown>"
    ) {
        val vertId: Int = compileShader(
            vertSrc, vertPath, GL_VERTEX_SHADER, "vertex"
        )
        val fragId: Int
        try {
            fragId = compileShader(
                fragSrc, fragPath, GL_FRAGMENT_SHADER, "fragment"
            )
        } finally {
            glDeleteShader(vertId)
        }
        val programId: Int = glCreateProgram()
        this.programId = programId
        glAttachShader(programId, vertId)
        glAttachShader(programId, fragId)
        glLinkProgram(programId)
        check(glGetProgrami(programId, GL_LINK_STATUS) == GL_TRUE) {
            val infoLog: String = glGetProgramInfoLog(programId)
            glDeleteProgram(programId)
            glDeleteShader(vertId)
            glDeleteShader(fragId)
            "Linking of shaders '$vertPath' and '$fragPath' failed:" +
                "\n\n$infoLog"
        }
        glDetachShader(programId, vertId)
        glDetachShader(programId, fragId)
        glDeleteShader(vertId)
        glDeleteShader(fragId)
    }
    
    private fun bind() {
        glBindProgram()
    }
    
}