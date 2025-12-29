
package schwalbe.ventura.engine.gfx

import schwalbe.ventura.engine.UsageAfterDisposalException
import schwalbe.ventura.engine.Disposable
import schwalbe.ventura.engine.Resource

import java.nio.ByteBuffer
import org.lwjgl.opengl.GL33.*
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.nio.ByteOrder

class Texture(
    val width: Int, val height: Int,
    val filter: Filter, val format: Format,
    data: ByteBuffer? = null
) : Disposable {
    
    enum class Format(val glFmt: Int, val glIFmt: Int, val glChType: Int) {
        R8      (GL_RED,    GL_R8,      GL_UNSIGNED_BYTE),
        RG8     (GL_RG,     GL_RG8,     GL_UNSIGNED_BYTE),
        RGB8    (GL_RGB,    GL_RGB8,    GL_UNSIGNED_BYTE),
        RGBA8   (GL_RGBA,   GL_RGBA8,   GL_UNSIGNED_BYTE),
        
        R16     (GL_RED,    GL_R16,     GL_UNSIGNED_SHORT),
        RG16    (GL_RG,     GL_RG16,    GL_UNSIGNED_SHORT),
        RGB16   (GL_RGB,    GL_RGB16,   GL_UNSIGNED_SHORT),
        RGBA16  (GL_RGBA,   GL_RGBA16,  GL_UNSIGNED_SHORT),
        
        R16F    (GL_RED,    GL_R16F,    GL_HALF_FLOAT),
        RG16F   (GL_RG,     GL_RG16F,   GL_HALF_FLOAT),
        RGB16F  (GL_RGB,    GL_RGB16F,  GL_HALF_FLOAT),
        RGBA16F (GL_RGBA,   GL_RGBA16F, GL_HALF_FLOAT),
        
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
    
    companion object
    
    
    var texId: Int? = null
        private set

    init {
        require(width >= 1 && height >= 1)
        val id: Int = glGenTextures()
        this.texId = id
        this.bind()
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

    internal fun bind() {
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, this.getTexId())
    }

    override fun dispose() {
        val oldId: Int = this.texId ?: return
        glDeleteTextures(oldId)
    }

}


fun Texture.Companion.loadImage(
    path: String, filter: Texture.Filter
) = Resource<Texture> {
    val imageBuffer: ByteBuffer?
    val width: Int
    val height: Int
    MemoryStack.stackPush().use { stack ->
        val widthPtr = stack.mallocInt(1)
        val heightPtr = stack.mallocInt(1)
        val channelsPtr = stack.mallocInt(1)
        stbi_set_flip_vertically_on_load(true)
        imageBuffer = stbi_load(path, widthPtr, heightPtr, channelsPtr, 4)
        width = widthPtr.get(0)
        height = heightPtr.get(0)
    }
    require(imageBuffer != null) { "Image file '$path' could not be read" }
    return@Resource {
        val format = Texture.Format.RGBA8
        val texture = Texture(width, height, filter, format, imageBuffer)
        stbi_image_free(imageBuffer)
        texture
    }
}

fun Texture.Companion.fromBufferedImage(
    image: BufferedImage, filter: Texture.Filter
): Texture {
    val pixels: IntArray = (image.raster.dataBuffer as DataBufferInt).data
    val buffer: ByteBuffer = ByteBuffer
        .allocateDirect(image.width * image.height * 4)
        .order(ByteOrder.nativeOrder())
    for (argb in pixels) {
        val a: Int = (argb shr 24) and 0xFF
        val r: Int = (argb shr 16) and 0xFF
        val g: Int = (argb shr  8) and 0xFF
        val b: Int =          argb and 0xFF
        buffer.put(r.toByte())
        buffer.put(g.toByte())
        buffer.put(b.toByte())
        buffer.put(a.toByte())
    }
    buffer.flip()
    return Texture(
        image.width, image.height, filter, Texture.Format.RGBA8, buffer
    )
}