
package schwalbe.ventura.editor

import schwalbe.ventura.client.Items
import schwalbe.ventura.client.Renderer
import schwalbe.ventura.client.game.World
import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.fromCallback
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
        editor.nav.clear(EditorScreen.create(editor))
    })
    editor.gameloop()
    editor.dispose()
}