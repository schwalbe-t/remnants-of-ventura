
package schwalbe.ventura.engine.audio

import org.joml.Vector3f
import org.joml.Vector3fc
import org.lwjgl.openal.AL10.*
import schwalbe.ventura.engine.Disposable
import schwalbe.ventura.engine.UsageAfterDisposalException

class AudioSource : Disposable {

    var sourceId: Int? = null
        private set

    init {
        this.sourceId = alGenSources()
    }

    fun getSourceId(): Int
        = this.sourceId ?: throw UsageAfterDisposalException()

    private val lastPosition = Vector3f()
    var position: Vector3fc
        get() = this.lastPosition
        set(v) {
            this.lastPosition.set(v)
            alSource3f(this.getSourceId(), AL_POSITION, v.x(), v.y(), v.z())
        }

    var gain: Float = 1f
        set(value) {
            field = value
            alSourcef(this.getSourceId(), AL_GAIN, value)
        }

    var pitch: Float = 1f
        set(value) {
            field = value
            alSourcef(this.getSourceId(), AL_PITCH, value)
        }

    fun stop(): Unit = alSourceStop(this.getSourceId())

    fun play(audio: Audio) {
        alSourcei(this.getSourceId(), AL_BUFFER, audio.getBufferId())
        alSourcePlay(this.getSourceId())
    }

    val isPlaying: Boolean
        get() = alGetSourcei(this.getSourceId(), AL_SOURCE_STATE) == AL_PLAYING

    override fun dispose() {
        alDeleteSources(this.sourceId ?: return)
        this.sourceId = null
    }

}
