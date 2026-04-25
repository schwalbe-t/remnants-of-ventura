
package schwalbe.ventura.client

import kotlinx.serialization.Serializable
import schwalbe.ventura.engine.audio.AudioListener

@Serializable
data class Settings(
    // audio settings
    var generalVolume: Float = 1f,
    var musicVolume: Float = 1f,
    var sfxVolume: Float = 1f,

    // display settings
    var fullscreenEnabled: Boolean = true,
    var vsyncEnabled: Boolean = true

    // graphics settings
)

fun Settings.applyAudio(client: Client): Settings {
    AudioListener.gain = this.generalVolume
    client.soundtrack.gain = this.musicVolume
    return this
}

fun Settings.applyDisplay(client: Client): Settings {
    client.window.setFullscreenEnabled(this.fullscreenEnabled)
    client.window.setVsyncEnabled(this.vsyncEnabled)
    return this
}
