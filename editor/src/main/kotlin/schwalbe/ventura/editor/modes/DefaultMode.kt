
package schwalbe.ventura.editor.modes

import schwalbe.ventura.editor.Editor
import schwalbe.ventura.editor.render
import schwalbe.ventura.editor.update

fun createDefaultMode(editor: Editor): () -> EditorMode = {
    val mode = EditorMode(
        render = {
            editor.update()
            editor.render()
        },
        navigator = editor.nav
    )
    mode
}
