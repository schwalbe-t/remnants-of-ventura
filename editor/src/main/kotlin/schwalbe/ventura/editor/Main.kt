
package schwalbe.ventura.editor

import schwalbe.ventura.client.*
import schwalbe.ventura.engine.*
import schwalbe.ventura.client.game.World
import schwalbe.ventura.engine.ui.loadUiResources
import java.nio.file.Path

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
        editor.nav.clear(createEditorScreen(editor))
    })
    editor.gameloop()
    editor.dispose()
}