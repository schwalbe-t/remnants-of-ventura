
package schwalbe.ventura.client

import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.engine.*
import schwalbe.ventura.client.screens.offline.mainScreen
import schwalbe.ventura.client.screens.submitScreenResources
import schwalbe.ventura.engine.ui.loadUiResources

private fun submitResources(resLoader: ResourceLoader) {
    loadUiResources(resLoader)
    submitScreenResources(resLoader)
    resLoader.submit(localized)
    Renderer.submitResources(resLoader)
    World.submitResources(resLoader)
    Items.submitResources(resLoader)
}

fun main() {
    val client = Client()
    client.loadResources()
    submitResources(client.resLoader)
    client.resLoader.submit(Resource.fromCallback {
        configureNavigator(client.nav)
        localized().changeLanguage(client.config.language)
        client.nav.clear(mainScreen(client))
    })
    client.gameloop()
    client.dispose()
}
