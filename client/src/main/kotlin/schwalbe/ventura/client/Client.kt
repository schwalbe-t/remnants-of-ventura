
package schwalbe.ventura.client

import schwalbe.ventura.client.screens.GameScreen
import schwalbe.ventura.client.game.World
import schwalbe.ventura.engine.*

class Client : Application<GameScreen>(
    window = Window(
        name = "Remnants of Ventura",
        sizeFactor = 0.75f,
        iconPath = "res/icon.png"
    )
) {

    val config: Config = Config.read()

    init {
        this.window.setVsyncEnabled(true)
    }

    val network = NetworkClient()

    val renderer = Renderer(this.out3d)
    var username: String = ""
    var world: World? = null


    override fun beforeRender() {
        this.nav.currentOrNull?.networkState()
        this.network.handlePackets(this.nav.currentOrNull?.packets)
    }

    override fun dispose() {
        super.dispose()
        this.network.dispose()
    }

}
