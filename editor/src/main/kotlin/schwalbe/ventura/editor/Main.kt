
package schwalbe.ventura.editor

import schwalbe.ventura.client.*
import schwalbe.ventura.engine.*
import schwalbe.ventura.client.game.World
import schwalbe.ventura.editor.modes.createDefaultMode
import schwalbe.ventura.engine.ui.loadUiResources

fun submitResources(resLoader: ResourceLoader) {
    loadUiResources(resLoader)
    Renderer.submitResources(resLoader)
    World.submitResources(resLoader)
    Items.submitResources(resLoader)
}

fun main() {
    val editor = Editor()
    editor.loadResources()
    submitResources(editor.resLoader)
    editor.resLoader.submit(Resource.fromCallback {
        editor.nav.clear(createDefaultMode(editor))
    })
    editor.gameloop()
    editor.dispose()
}