
package schwalbe.ventura.engine.gfx

import schwalbe.ventura.engine.Disposable
import schwalbe.ventura.engine.UsageAfterDisposalException
import schwalbe.ventura.engine.Resource

import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.FloatBuffer
import java.nio.file.Paths
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryStack
import org.joml.*

class UniformBuffer(val writeHint: BufferWriteFreq) : Disposable {
    
    companion object {
        internal val bound = BindingManager<UniformBuffer>(UniformBuffer::bind)
    }
    
    var bufferId: Int? = glGenBuffers()
        private set
    var lastSize: Int? = null
        private set
        
    fun getBufferId(): Int = this.bufferId
        ?: throw UsageAfterDisposalException()
        
    fun write(data: ByteBuffer): UniformBuffer {
        UniformBuffer.bound.bindLazy(this)
        val lastSize: Int? = this.lastSize
        if (lastSize == null || data.remaining() > lastSize) {
            this.lastSize = data.remaining()
            glBufferData(GL_UNIFORM_BUFFER, data, this.writeHint.glValue)
        } else {
            glBufferSubData(GL_UNIFORM_BUFFER, 0, data)
        }
        return this
    }

    private fun bind() {
        glBindBuffer(GL_UNIFORM_BUFFER, this.getBufferId())
    }
        
    override fun dispose() {
        val id: Int = this.bufferId ?: return
        glDeleteBuffers(id)
        this.bufferId = null
    }
    
}

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

private class SlotManager<T> {
    
    data class Slot<T>(val index: Int, val value: T)
    
    private val slots: MutableMap<String, Slot<T>> = mutableMapOf()

    fun allocate(name: String, value: T): Int
        = this.slots
        .getOrPut(name) { Slot(this.slots.count(), value) }
        .index
    
    fun getAll(): Map<String, Slot<T>> = this.slots

}

class Shader : Disposable {
    
    companion object {
        internal val bound = BindingManager<Shader>(Shader::bind)
    }
    
    var programId: Int? = null
        private set
    private val cachedUniforms: MutableMap<String, Int> = mutableMapOf()
    private val textures: SlotManager<Texture> = SlotManager()
    private val buffers: SlotManager<UniformBuffer> = SlotManager()
        
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
    
    fun getProgramId(): Int
        = this.programId ?: throw UsageAfterDisposalException()
    
    /**
     * Partially binds the current shader program, invalidating any bound
     * shaders apart from the shader the method is called on.
     * Attempts to get the location of the given uniform (or uniform block,
     * depending on what is passed as [locationGetter]).
     * This method will cache the returned locations.
     * Null is returned if the uniform doesn't exist in the shader. NULL DOES
     * NOT INDICATE FAILURE AND MUST BE IGNORED.
     */
    private fun getUniformLocation(
        name: String, locationGetter: (Int, String) -> Int
    ): Int? {
        Shader.bound.invalidateUnless(this)
        val programId: Int = this.getProgramId()
        glUseProgram(programId)
        var loc: Int? = this.cachedUniforms[name]
        if (loc == null) {
            loc = locationGetter(programId, name)
            this.cachedUniforms[name] = loc
        }
        // may also return no index if the specified uniform exists in the
        // shader source, but was optimized away during compilation
        return if (loc != GL_INVALID_INDEX) { loc } else { null }
    }
        
    private inline fun setNormal(name: String, setter: (Int) -> Unit) {
        val loc: Int = this.getUniformLocation(name, ::glGetUniformLocation)
            ?: return
        setter(loc)
    }
    
    /**
     * Assigns an array value to the shader uniform specified by [name] by
     * creating a buffer using [createBuffer]. The size passed to this buffer
     * is the number of elements in [values] multiplied by the [valueSize].
     * The buffer is then filled by executing [fillBuffer] for each element
     * in [values], where the index is the index from [values] multiplied
     * by the [valueSize] (meaning the offset in the target buffer).
     * The buffer is then applied as the value for the uniform by executing the
     * [useBuffer] function.
     */
    private inline fun <B, V> setBuffer(
        name: String, values: Iterable<V>, valueSize: Int,
        createBuffer: (MemoryStack, Int) -> B, 
        fillBuffer: (V, Int, B) -> Unit,
        useBuffer: (Int, B) -> Unit
    ) {
        this.setNormal(name) { loc ->
            MemoryStack.stackPush().use { stack ->
                val b: B = createBuffer(stack, values.count() * valueSize)
                values.withIndex()
                    .forEach { (i, v) -> fillBuffer(v, i * 4, b) }
                useBuffer(loc, b)
            }
        }
    }
    
    fun setFloat(name: String, v: Float)
        = this.setNormal(name) { glUniform1f(it, v) }
    fun setVec2(name: String, v: Vector2fc)
        = this.setNormal(name) { glUniform2f(it, v.x(), v.y()) }
    fun setVec3(name: String, v: Vector3fc)
        = this.setNormal(name) { glUniform3f(it, v.x(), v.y(), v.z()) }
    fun setVec4(name: String, v: Vector4fc)
        = this.setNormal(name) { glUniform4f(it, v.x(), v.y(), v.z(), v.w()) }
    fun setFloatArr(name: String, v: Iterable<Float>) = this.setBuffer(
        name, v, valueSize = 1, MemoryStack::mallocFloat,
        fillBuffer = { v, i, b -> b.put(i, v) }, ::glUniform1fv
    )
    fun setVec2Arr(name: String, v: Iterable<Vector2fc>) = this.setBuffer(
        name, v, valueSize = 2, MemoryStack::mallocFloat,
        fillBuffer = Vector2fc::get, ::glUniform2fv
    )
    fun setVec3Arr(name: String, v: Iterable<Vector3fc>) = this.setBuffer(
        name, v, valueSize = 3, MemoryStack::mallocFloat,
        fillBuffer = Vector3fc::get, ::glUniform3fv
    )
    fun setVec4Arr(name: String, v: Iterable<Vector4fc>) = this.setBuffer(
        name, v, valueSize = 4, MemoryStack::mallocFloat,
        fillBuffer = Vector4fc::get, ::glUniform4fv
    )
    
    fun setInt(name: String, v: Int)
        = this.setNormal(name) { glUniform1i(it, v) }
    fun setIvec2(name: String, v: Vector2ic)
        = this.setNormal(name) { glUniform2i(it, v.x(), v.y()) }
    fun setIvec3(name: String, v: Vector3ic)
        = this.setNormal(name) { glUniform3i(it, v.x(), v.y(), v.z()) }
    fun setIvec4(name: String, v: Vector4ic)
        = this.setNormal(name) { glUniform4i(it, v.x(), v.y(), v.z(), v.w()) }
    fun setIntArr(name: String, v: Iterable<Int>) = this.setBuffer(
        name, v, valueSize = 1, MemoryStack::mallocInt,
        fillBuffer = { v, i, b -> b.put(i, v) }, ::glUniform1iv
    )
    fun setIvec2Arr(name: String, v: Iterable<Vector2ic>) = this.setBuffer(
        name, v, valueSize = 2, MemoryStack::mallocInt,
        fillBuffer = Vector2ic::get, ::glUniform2iv
    )
    fun setIvec3Arr(name: String, v: Iterable<Vector3ic>) = this.setBuffer(
        name, v, valueSize = 3, MemoryStack::mallocInt,
        fillBuffer = Vector3ic::get, ::glUniform3iv
    )
    fun setIvec4Arr(name: String, v: Iterable<Vector4ic>) = this.setBuffer(
        name, v, valueSize = 4, MemoryStack::mallocInt,
        fillBuffer = Vector4ic::get, ::glUniform4iv
    )
        
    fun setBool(name: String, v: Boolean)
        = this.setNormal(name) { glUniform1i(it, if (v) { 1 } else { 0 }) }
    fun setBoolArr(name: String, v: Iterable<Boolean>) = this.setBuffer(
        name, v, valueSize = 1, MemoryStack::mallocInt,
        fillBuffer = { v, i, b -> b.put(i, if (v) { 1 } else { 0 }) },
        ::glUniform1iv
    )
    
    fun setMat3(name: String, v: Matrix3fc) = this.setMat3Arr(name, listOf(v))
    fun setMat4(name: String, v: Matrix4fc) = this.setMat4Arr(name, listOf(v))
    fun setMat3Arr(name: String, v: Iterable<Matrix3fc>) = this.setBuffer(
        name, v, valueSize = 3 * 3, MemoryStack::mallocFloat,
        fillBuffer = Matrix3fc::get,
        useBuffer = { loc, b -> glUniformMatrix3fv(loc, false, b) }
    )
    fun setMat4Arr(name: String, v: Iterable<Matrix4fc>) = this.setBuffer(
        name, v, valueSize = 4 * 4, MemoryStack::mallocFloat,
        fillBuffer = Matrix4fc::get,
        useBuffer = { loc, b -> glUniformMatrix4fv(loc, false, b) }
    )
    
    fun setSampler2D(name: String, v: Texture) {
        val loc: Int = this.getUniformLocation(name, ::glGetUniformLocation)
            ?: return
        val slot: Int = this.textures.allocate(name, v)
        glActiveTexture(GL_TEXTURE0 + slot)
        Texture.bound.bindEager(v)
        glUniform1i(loc, slot)
    }
    
    fun setBuffer(name: String, v: UniformBuffer) {
        val loc: Int = this.getUniformLocation(name, ::glGetUniformBlockIndex)
            ?: return
        val bufferId: Int = v.getBufferId()
        val point: Int = this.buffers.allocate(name, v)
        glBindBufferBase(GL_UNIFORM_BUFFER, point, bufferId)
        glUniformBlockBinding(this.getProgramId(), loc, point)
    }
    
    private fun bind() {
        glUseProgram(this.getProgramId())
        for ((slot, texture) in this.textures.getAll().values) {
            glActiveTexture(GL_TEXTURE0 + slot)
            Texture.bound.bindEager(texture)
        }
        for ((point, buffer) in this.buffers.getAll().values) {
            glBindBufferBase(GL_UNIFORM_BUFFER, point, buffer.getBufferId())
        }
    }
    
    override fun dispose() {
        val programId: Int = this.programId ?: return
        this.programId = null
        glDeleteProgram(programId)
    }
    
}


const val SHADER_VERSION_STRING: String = "#version 330 core"

private fun processShaderLine(
    line: String, path: String, dir: String, bannedIncludes: MutableSet<String>
): String {
    if (line.startsWith("#pragma once")) {
        bannedIncludes.add(path)
        return ""
    }
    val inclStart = "#include \""
    if (!line.startsWith(inclStart)) { return line }
    val endIdx: Int = line.indexOf("\"", inclStart.length)
    if (endIdx == -1) { return line }
    val inclRelPath: String = line.substring(inclStart.length, endIdx)
    val inclPath: String = Paths.get(dir, inclRelPath).toString()
    val inclFile = File(inclPath)
    val inclAbsPath: String = inclFile.absolutePath
    val inclAbsDir: String = inclFile.parentFile.absolutePath
    if (bannedIncludes.contains(inclAbsPath)) {
        return ""
    }
    val inclContents: List<String>
    try {
        inclContents = inclFile.readLines()
    } catch (e: IOException) {
        throw IllegalArgumentException(
            "Shader file '$path' attempted to include '$inclPath'" +
                ", which could not be read"
        )
    }
    return inclContents.joinToString("\n") { l ->
        processShaderLine(l, inclAbsPath, inclAbsDir, bannedIncludes)    
    }
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
    val absDir: String = file.parentFile.absolutePath
    val bannedIncludes = mutableSetOf<String>()
    val processed: String = contents.joinToString("\n") {
        l -> processShaderLine(l, absPath, absDir, bannedIncludes)
    }
    return SHADER_VERSION_STRING + "\n" + processed
}

fun Shader.Companion.loadGlsl(
    vertPath: String, fragPath: String
) = Resource<Shader> {
    val vertSrc: String = readExpandBaseShader(vertPath)
    val fragSrc: String = readExpandBaseShader(fragPath)
    return@Resource { Shader(vertSrc, fragSrc, vertPath, fragPath) }
}