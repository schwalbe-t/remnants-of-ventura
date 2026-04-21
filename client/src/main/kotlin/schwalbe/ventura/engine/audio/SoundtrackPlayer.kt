
package schwalbe.ventura.engine.audio

import schwalbe.ventura.engine.Disposable
import schwalbe.ventura.engine.Resource

class SoundtrackPlayer : Disposable {

    companion object {
        const val FADEOUT_SECONDS: Float = 1f
    }

    val source = AudioSource()

    var activeTracklist: List<() -> Audio> = listOf()
        private set
    var activeFadeoutGain: Float = 1f
        private set

    var gain: Float = 1f

    var nextTracklist: List<() -> Audio>? = null
        private set

    fun changeTracklist(newList: List<() -> Audio>?) {
        if (this.nextTracklist === newList) {
            return
        }
        if (this.activeTracklist === newList && this.nextTracklist === null) {
            return
        }
        this.nextTracklist = newList
        this.activeFadeoutGain = 1f
    }

    private var lastUpdate: Long = System.currentTimeMillis()

    private fun computeTimeDelta(): Float {
        val now: Long = System.currentTimeMillis()
        val delta: Float = (now - this.lastUpdate).toFloat() / 1000f
        this.lastUpdate = now
        return delta
    }

    private fun fadeToNextTracklist(deltaTime: Float) {
        val nextTracklist: List<() -> Audio> = this.nextTracklist ?: return
        this.activeFadeoutGain -= deltaTime / FADEOUT_SECONDS
        if (this.activeFadeoutGain > 0f) { return }
        this.source.stop()
        this.activeTracklist = nextTracklist
        this.nextTracklist = null
        this.activeFadeoutGain = 1f
    }

    private fun playNextTrack() {
        if (this.source.isPlaying) { return }
        val track: () -> Audio = this.activeTracklist.randomOrNull() ?: return
        this.source.play(track())
    }

    fun update() {
        val deltaTime: Float = this.computeTimeDelta()
        this.fadeToNextTracklist(deltaTime)
        this.source.gain = this.gain * this.activeFadeoutGain
        this.playNextTrack()
    }

    override fun dispose() {
        this.source.dispose()
    }

}

fun tracklistOf(vararg tracks: Resource<Audio>): List<() -> Audio>
    = tracks.map { { it() } }
