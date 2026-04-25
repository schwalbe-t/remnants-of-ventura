
package schwalbe.ventura.client

import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.offline.loadLoadingScreenResources
import schwalbe.ventura.client.screens.offline.loadingScreen
import schwalbe.ventura.client.screens.offline.serverSelectScreen
import schwalbe.ventura.client.screens.submitScreenResources
import schwalbe.ventura.engine.*
import schwalbe.ventura.engine.ui.loadUiResources

private fun submitPreloadResources(resLoader: ResourceLoader) {
    loadUiResources(resLoader)
    loadNavigatorResources(resLoader)
    loadLoadingScreenResources(resLoader)
    resLoader.submit(localized)
}

private fun submitGameResources(resLoader: ResourceLoader) {
    submitScreenResources(resLoader)
    Soundtrack.submitResources(resLoader)
    Renderer.submitResources(resLoader)
    World.submitResources(resLoader)
    Items.submitResources(resLoader)
}

fun main() {
    val client = Client()
    submitPreloadResources(client.resLoader)
    client.resLoader.submit(Resource.fromCallback {
        client.window.setVisible()
        configureNavigator(client.nav)
        localized().changeLanguage(client.config.language)
        client.config.settings
            .applyDisplay(client)
            .applyAudio(client)
        client.nav.clear(loadingScreen(client))
        submitGameResources(client.resLoader)
        client.resLoader.submit(Resource.fromCallback {
            client.nav.clear(serverSelectScreen(client))
        })
    })
    client.loadResources()
    client.gameloop()
    client.dispose()
}
