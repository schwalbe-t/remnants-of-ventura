
package schwalbe.ventura.editor.modes

import schwalbe.ventura.client.ApplicationScreen
import schwalbe.ventura.engine.ui.*

class EditorMode(
    override val render: () -> Unit = {},
    navigator: UiNavigator<EditorMode>,
    onOpen: () -> Unit = {},
    onClose: () -> Unit = {}
) : UiScreen<EditorMode>(navigator, onOpen, onClose),
    ApplicationScreen<EditorMode>
