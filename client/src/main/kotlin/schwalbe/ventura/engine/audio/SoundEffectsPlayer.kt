
package schwalbe.ventura.engine.audio

import org.joml.Vector3f
import schwalbe.ventura.engine.Disposable
import org.joml.Vector3fc

class SoundEffectsPlayer(
    sourcesCount: Int = 16
) : Disposable {

    companion object {
        val ORIGIN: Vector3fc = Vector3f(0f, 0f, 0f)
    }

    private val sources: Array<AudioSource>
        = Array(sourcesCount) { AudioSource() }
    private var nextSourceIdx: Int = 0

    private fun allocateSource(): AudioSource {
        val sourceIdx: Int = this.nextSourceIdx
        this.nextSourceIdx = (sourceIdx + 1) % this.sources.size
        return this.sources[sourceIdx]
    }

    var gain: Float = 1f

    fun play(effect: Audio, position: Vector3fc = ORIGIN, gain: Float = 1f) {
        val source = this.allocateSource()
        source.position = position
        source.gain = this.gain * gain
        source.play(effect)
    }

    override fun dispose() {
        this.sources.forEach(AudioSource::dispose)
    }

}