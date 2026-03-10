
package schwalbe.ventura.editor

import schwalbe.ventura.client.ApplicationScreen
import schwalbe.ventura.engine.ui.UiNavigator
import schwalbe.ventura.engine.ui.UiScreen

class EditorScreen(
    override val render: () -> Unit = {},
    navigator: UiNavigator<EditorScreen>,
    onOpen: () -> Unit = {},
    onClose: () -> Unit = {}
) : UiScreen<EditorScreen>(navigator, onOpen, onClose),
    ApplicationScreen<EditorScreen>

fun createEditorScreen(editor: Editor): () -> EditorScreen = {
    val screen = EditorScreen(
        render = {
            editor.update()
            editor.render()
        },
        navigator = editor.nav
    )
    screen
}
