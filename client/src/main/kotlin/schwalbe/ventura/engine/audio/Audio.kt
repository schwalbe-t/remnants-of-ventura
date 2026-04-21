
package schwalbe.ventura.engine.audio

import org.lwjgl.openal.AL10.*
import org.lwjgl.stb.STBVorbis.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.libc.LibCStdlib
import schwalbe.ventura.engine.Disposable
import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.UsageAfterDisposalException
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import java.nio.file.Files
import java.nio.file.Path

class Audio(pcm: ShortBuffer, channels: Int, sampleRate: Int) : Disposable {
    companion object

    var bufferId: Int? = null
        private set

    init {
        val format = when (channels) {
            1 -> AL_FORMAT_MONO16
            2 -> AL_FORMAT_STEREO16
            else -> error("Failed to create audio with $channels channels")
        }
        val bufferId: Int = alGenBuffers()
        alBufferData(bufferId, format, pcm, sampleRate)
        this.bufferId = bufferId
    }

    fun getBufferId(): Int
        = this.bufferId ?: throw UsageAfterDisposalException()

    override fun dispose() {
        alDeleteBuffers(this.bufferId ?: return)
        this.bufferId = null
    }

}

fun Audio.Companion.loadOgg(path: String) = Resource<Audio>(allowReset = true) {
    val bytes: ByteBuffer = Files.readAllBytes(Path.of(path)).let {
        MemoryUtil.memAlloc(it.size).put(it).flip()
    }
    val pcm: ShortBuffer
    val channels: Int
    val sampleRate: Int
    MemoryStack.stackPush().use { stack ->
        val channelsPtr = stack.mallocInt(1)
        val sampleRatePtr = stack.mallocInt(1)
        pcm = stb_vorbis_decode_memory(bytes, channelsPtr, sampleRatePtr)
            ?: error("Audio file '$path' could not be decoded")
        channels = channelsPtr.get(0)
        sampleRate = sampleRatePtr.get(0)
        MemoryUtil.memFree(bytes)
    }
    return@Resource {
        val audio = Audio(pcm, channels, sampleRate)
        LibCStdlib.free(pcm)
        audio
    }
}
