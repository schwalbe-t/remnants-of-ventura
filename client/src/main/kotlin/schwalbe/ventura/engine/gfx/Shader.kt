
package schwalbe.ventura.engine.gfx

import schwalbe.ventura.engine.Disposable
import schwalbe.ventura.engine.UsageAfterDisposalException
import schwalbe.ventura.engine.Resource

import java.io.File
import java.io.IOException
import org.lwjgl.opengl.GL33.*

private fun withCompiledShader(
    src: String, path: String, glType: Int, dispType: String,
    f: (Int) -> Unit
) {
    val shaderId: Int = glCreateShader(glType)
    glShaderSource(shaderId, src)
    glCompileShader(shaderId)
    check(glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_TRUE) {
        val infoLog: String = glGetShaderInfoLog(shaderId)
        glDeleteShader(shaderId)
        "Compilation of $dispType shader '$path' failed:\n\n$infoLog"
    }
    try {
        f(shaderId)
    } finally {
        glDeleteShader(shaderId)
    }
}

private fun linkShaderProgram(
    vertPath: String, vertId: Int, fragPath: String, fragId: Int
): Int {
    val programId: Int = glCreateProgram()
    glAttachShader(programId, vertId)
    glAttachShader(programId, fragId)
    glLinkProgram(programId)
    check(glGetProgrami(programId, GL_LINK_STATUS) == GL_TRUE) {
        val infoLog: String = glGetProgramInfoLog(programId)
        glDeleteProgram(programId)
        "Linking of shaders '$vertPath' and '$fragPath' failed:" +
            "\n\n$infoLog"
    }
    glDetachShader(programId, vertId)
    glDetachShader(programId, fragId)
    return programId
}

class Shader : Disposable {
    
    companion object {
        internal val bound = BindingManager<Shader>(Shader::bind)
    }
    
    var programId: Int? = null
        private set

    constructor(
        vertSrc: String, fragSrc: String,
        vertPath: String = "<unknown>", fragPath: String = "<unknown>"
    ) {
        withCompiledShader(
            vertSrc, vertPath, GL_VERTEX_SHADER, "vertex"
        ) { vertId ->
            withCompiledShader(
                fragSrc, fragPath, GL_FRAGMENT_SHADER, "fragment"
            ) { fragId ->
                this.programId = linkShaderProgram(
                    vertPath, vertId, fragPath, fragId
                )
            }
        }
    }
    
    private fun bind() {
        val programId: Int = this.programId
            ?: throw UsageAfterDisposalException()
        glUseProgram(programId)
    }
    
    override fun dispose() {
        val programId: Int = this.programId ?: return
        this.programId = null
        glDeleteProgram(programId)
    }
    
}


private fun expandShaderLine(line: String, path: String): String {
    val inclStart = "#include \""
    if (!line.startsWith(inclStart)) { return line }
    val endIdx: Int = line.indexOf("\"", inclStart.length)
    if (endIdx == -1) { return line }
    val inclPath: String = line.substring(inclStart.length, endIdx)
    val inclFile = File(inclPath)
    val inclContents: List<String>
    try {
        inclContents = inclFile.readLines()
    } catch (e: IOException) {
        throw IllegalArgumentException(
            "Shader file '$path' attempted to include '$inclPath'" +
                ", which could not be read"
        )
    }
    val inclAbsPath: String = inclFile.absolutePath
    return inclContents
        .joinToString("\n") { l -> expandShaderLine(l, inclAbsPath) }
}

private fun readExpandBaseShader(path: String): String {
    val file = File(path)
    val contents: List<String>
    try {
        contents = file.readLines()
    } catch (e: IOException) {
        throw IllegalArgumentException("Shader file '$path' could not be read")
    }
    val absPath: String = file.absolutePath
    return contents
        .joinToString("\n") { l -> expandShaderLine(l, absPath) }
}

fun Shader.Companion.loadGlsl(
    vertPath: String, fragPath: String
) = Resource<Shader> {
    val vertSrc: String = readExpandBaseShader(vertPath)
    val fragSrc: String = readExpandBaseShader(fragPath)
    return@Resource { Shader(vertSrc, fragSrc, vertPath, fragPath) }
}