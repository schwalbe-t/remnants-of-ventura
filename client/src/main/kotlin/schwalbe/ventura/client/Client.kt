
package schwalbe.ventura.client

import schwalbe.ventura.client.screens.GameScreen
import schwalbe.ventura.client.game.ChatMessageBuffer
import schwalbe.ventura.client.game.World
import schwalbe.ventura.engine.*
import schwalbe.ventura.engine.audio.SoundtrackPlayer
import schwalbe.ventura.engine.input.*

class Client : Application<GameScreen>(
    window = Window(
        name = "Remnants of Ventura",
        sizeFactor = 0.75f,
        iconPath = "res/icon.png"
    ),
    shouldReloadResources = { Key.LEFT_CONTROL.isPressed && Key.R.wasPressed }
) {

    val config: Config = Config.read()

    val network = NetworkClient()
    val messages = ChatMessageBuffer()

    val soundtrack = SoundtrackPlayer()
    val renderer = Renderer(this.out3d)
    var username: String = ""
    var world: World? = null


    override fun beforeRender() {
        this.nav.currentOrNull?.networkState()
        this.network.handlePackets(this.nav.currentOrNull?.packets)
        this.soundtrack.update()
    }

    override fun dispose() {
        super.dispose()
        this.network.dispose()
        this.soundtrack.dispose()
        this.renderer.dispose()
        this.world?.dispose()
    }

}
