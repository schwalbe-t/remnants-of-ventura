
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

class ShaderSlotManager<T>(val firstSlot: Int) {
    
    data class Slot<T>(val index: Int, val value: T)
    
    private val slots: MutableMap<String, Slot<T>> = mutableMapOf()

    fun allocate(name: String, value: T): Int {
        val existing: Slot<T>? = this.slots[name]
        val newIndex: Int = existing?.index
            ?: (this.firstSlot + this.slots.count())
        this.slots[name] = Slot(newIndex, value)
        return newIndex
    }
    
    fun getAll(): Map<String, Slot<T>> = this.slots

}

open class Uniform<S : Uniforms<S>, T>(
    val name: String,
    val setter: (Shader<*, *>, T) -> Unit
)

class ArrayUniform<S : Uniforms<S>, T>(
    name: String,
    val maxCount: Int,
    setter: (Shader<*, *>, Iterable<T>) -> Unit
) : Uniform<S, Iterable<T>>(name, setter)

interface Uniforms<S : Uniforms<S>> {
    
    private fun <T> standardUniform(
        name: String, setter: (Int, T) -> Unit
    ) = Uniform<S, T>(name) { shader, value ->
        val loc: Int = shader.getUniformLocation(name, ::glGetUniformLocation)
            ?: return@Uniform
        setter(loc, value)
    }
    
    private fun <T> arrayUniform(
        name: String, maxCount: Int, setter: (Int, Iterable<T>) -> Unit
    ) = ArrayUniform<S, T>(name, maxCount) { shader, values ->
        require(values.count() <= maxCount) {
            "Array uniform '$name' has capacity $maxCount, but was assigned" +
                " ${values.count()} value(s)"
        }
        val loc: Int = shader.getUniformLocation(name, ::glGetUniformLocation)
            ?: return@ArrayUniform
        setter(loc, values)
    }
    
    /**
     * Assigns an array value to the shader uniform specified by [name] by
     * creating a buffer using [createBuffer]. The size passed to this buffer
     * is the number of elements in the values multiplied by the [valueSize].
     * The buffer is then filled by executing [fillBuffer] for each element
     * in the values, where the index is the index from the values multiplied
     * by the [valueSize] (meaning the offset in the target buffer).
     * The buffer is then applied as the value for the uniform by executing the
     * [useBuffer] function.
     */
    private fun <B, V> bufferUniform(
        name: String, maxCount: Int, valueSize: Int,
        createBuffer: (MemoryStack, Int) -> B,
        fillBuffer: (V, Int, B) -> Unit,
        useBuffer: (Int, B) -> Unit
    ) = arrayUniform<V>(name, maxCount) { loc, values ->
        MemoryStack.stackPush().use { stack ->
            val b: B = createBuffer(stack, values.count() * valueSize)
            values.withIndex()
                .forEach { (i, v) -> fillBuffer(v, i * 4, b) }
            useBuffer(loc, b)
        }
    }
    
    fun float(name: String) = this.standardUniform<Float>(name, ::glUniform1f)
    fun vec2(name: String) = this.standardUniform<Vector2fc>(name) { loc, v ->
        glUniform2f(loc, v.x(), v.y())
    }
    fun vec3(name: String) = this.standardUniform<Vector3fc>(name) { loc, v ->
        glUniform3f(loc, v.x(), v.y(), v.z())
    }
    fun vec4(name: String) = this.standardUniform<Vector4fc>(name) { loc, v ->
        glUniform4f(loc, v.x(), v.y(), v.z(), v.w())
    }
    fun floatArr(name: String, maxCount: Int) = this.bufferUniform<FloatBuffer, Float>(
        name, maxCount, valueSize = 1, MemoryStack::mallocFloat,
        fillBuffer = { v, i, b -> b.put(i, v) }, ::glUniform1fv
    )
    fun vec2Arr(name: String, maxCount: Int) = this.bufferUniform<FloatBuffer, Vector2fc>(
        name, maxCount, valueSize = 2, MemoryStack::mallocFloat,
        fillBuffer = Vector2fc::get, ::glUniform2fv
    )
    fun vec3Arr(name: String, maxCount: Int) = this.bufferUniform<FloatBuffer, Vector3fc>(
        name, maxCount, valueSize = 3, MemoryStack::mallocFloat,
        fillBuffer = Vector3fc::get, ::glUniform3fv
    )
    fun vec4Arr(name: String, maxCount: Int) = this.bufferUniform<FloatBuffer, Vector4fc>(
        name, maxCount, valueSize = 4, MemoryStack::mallocFloat,
        fillBuffer = Vector4fc::get, ::glUniform4fv
    )
    
    fun int(name: String) = this.standardUniform<Int>(name, ::glUniform1i)
    fun ivec2(name: String) = this.standardUniform<Vector2ic>(name) { loc, v ->
        glUniform2i(loc, v.x(), v.y())
    }
    fun ivec3(name: String) = this.standardUniform<Vector3ic>(name) { loc, v ->
        glUniform3i(loc, v.x(), v.y(), v.z())
    }
    fun ivec4(name: String) = this.standardUniform<Vector4ic>(name) { loc, v ->
        glUniform4i(loc, v.x(), v.y(), v.z(), v.w())
    }
    fun intArr(name: String, maxCount: Int) = this.bufferUniform<IntBuffer, Int>(
        name, maxCount, valueSize = 1, MemoryStack::mallocInt,
        fillBuffer = { v, i, b -> b.put(i, v) }, ::glUniform1iv
    )
    fun ivec2Arr(name: String, maxCount: Int) = this.bufferUniform<IntBuffer, Vector2ic>(
        name, maxCount, valueSize = 2, MemoryStack::mallocInt,
        fillBuffer = Vector2ic::get, ::glUniform2iv
    )
    fun ivec3Arr(name: String, maxCount: Int) = this.bufferUniform<IntBuffer, Vector3ic>(
        name, maxCount, valueSize = 3, MemoryStack::mallocInt,
        fillBuffer = Vector3ic::get, ::glUniform3iv
    )
    fun ivec4Arr(name: String, maxCount: Int) = this.bufferUniform<IntBuffer, Vector4ic>(
        name, maxCount, valueSize = 4, MemoryStack::mallocInt,
        fillBuffer = Vector4ic::get, ::glUniform4iv
    )
    
    fun bool(name: String) = this.standardUniform<Boolean>(name) { loc, v ->
        glUniform1i(loc, if (v) { 1 } else { 0 })
    }
    fun boolArr(name: String, maxCount: Int) = this.bufferUniform<IntBuffer, Boolean>(
        name, maxCount, valueSize = 1, MemoryStack::mallocInt,
        fillBuffer = { v, i, b -> b.put(i, if (v) { 1 } else { 0 }) },
        ::glUniform1iv
    )
    
    fun mat3(name: String): Uniform<S, Matrix3fc> {
        val underlying = mat3Arr(name, 1)
        val value: MutableList<Matrix3fc> = mutableListOf(Matrix3f())
        return Uniform<S, Matrix3fc>(name) { shader, v ->
            value[0] = v
            underlying.setter(shader, value)
        }
    }
    fun mat4(name: String): Uniform<S, Matrix4fc> {
        val underlying = mat4Arr(name, 1)
        val value: MutableList<Matrix4fc> = mutableListOf(Matrix4f())
        return Uniform<S, Matrix4fc>(name) { shader, v ->
            value[0] = v
            underlying.setter(shader, value)
        }
    }
    fun mat3Arr(name: String, maxCount: Int) = this.bufferUniform<FloatBuffer, Matrix3fc>(
        name, maxCount, valueSize = 3 * 3, MemoryStack::mallocFloat,
        fillBuffer = Matrix3fc::get,
        useBuffer = { loc, b -> glUniformMatrix3fv(loc, false, b) }
    )
    fun mat4Arr(name: String, maxCount: Int) = this.bufferUniform<FloatBuffer, Matrix4fc>(
        name, maxCount, valueSize = 4 * 4, MemoryStack::mallocFloat,
        fillBuffer = Matrix4fc::get,
        useBuffer = { loc, b -> glUniformMatrix4fv(loc, false, b) }
    )
    
    fun sampler2D(name: String) = Uniform<S, Texture>(name) { s, v ->
        val loc: Int = s.getUniformLocation(name, ::glGetUniformLocation)
            ?: return@Uniform
        val slot: Int = s.textures.allocate(name, v)
        glUniform1i(loc, slot)
    }
    
    fun block(name: String) = Uniform<S, UniformBuffer>(name) { s, v ->
        val loc: Int = s.getUniformLocation(name, ::glGetUniformBlockIndex)
            ?: return@Uniform
        val bufferId: Int = v.getBufferId()
        val point: Int = s.buffers.allocate(name, v)
        glBindBufferBase(GL_UNIFORM_BUFFER, point, bufferId)
        glUniformBlockBinding(s.getProgramId(), loc, point)
    }

}

interface ShaderDef<S : ShaderDef<S>> : Uniforms<S> {
    val path: String
}

interface VertShaderDef<S : ShaderDef<S>> : ShaderDef<S>
interface FragShaderDef<S : ShaderDef<S>> : ShaderDef<S>

class Shader<V : VertShaderDef<V>, F : FragShaderDef<F>> : Disposable {
    
    companion object
    
    
    var programId: Int? = null
        private set
    private val cachedUniforms: MutableMap<String, Int> = mutableMapOf()
    val textures: ShaderSlotManager<Texture>
        = ShaderSlotManager(firstSlot = 1) // slot 0 reserved
    val buffers: ShaderSlotManager<UniformBuffer>
        = ShaderSlotManager(firstSlot = 0)

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
    fun getUniformLocation(
        name: String, locationGetter: (Int, String) -> Int
    ): Int? {
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
    
    @JvmName("setVertUniform")
    operator fun <T> set(uniform: Uniform<V, T>, value: T) {
        uniform.setter(this, value)
    }
    
    @JvmName("setFragUniform")
    operator fun <T> set(uniform: Uniform<F, T>, value: T) {
        uniform.setter(this, value)
    }
    
    internal fun bind() {
        glUseProgram(this.getProgramId())
        for ((slot, texture) in this.textures.getAll().values) {
            glActiveTexture(GL_TEXTURE0 + slot)
            glBindTexture(GL_TEXTURE_2D, texture.getTexId())
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

fun <V : VertShaderDef<V>, F : FragShaderDef<F>> Shader.Companion.loadGlsl(
    vertDef: V, fragDef: F
) = Resource<Shader<V, F>> {
    val vertSrc: String = readExpandBaseShader(vertDef.path)
    val fragSrc: String = readExpandBaseShader(fragDef.path)
    return@Resource { Shader(vertSrc, fragSrc, vertDef.path, fragDef.path) }
}