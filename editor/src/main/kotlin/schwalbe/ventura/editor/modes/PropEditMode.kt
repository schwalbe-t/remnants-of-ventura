
package schwalbe.ventura.editor.modes

import schwalbe.ventura.editor.*
import schwalbe.ventura.engine.ui.*

private class ObjectPropEditor {
    companion object {
        val HEIGHT: UiSize = 3.5.vmin
    }
}

fun propEditMode(editor: Editor): () -> EditorMode = {
    val propList: SizedAxis = Axis.column(ObjectPropEditor.HEIGHT)
    propList.add(50.ph, Space())
    val mode = EditorMode(
        render = {
            editor.update()
            editor.render()
        },
        navigator = editor.nav
    )
    mode.add(layer = 0, element = Axis.row()
        .add(1f/3f * fpw, Stack()
            .add(FlatBackground().withColor(BACKGROUND_COLOR))
            .add(propList
                .wrapScrolling(horiz = false, vert = true)
            )
        )
    )
    mode
}
