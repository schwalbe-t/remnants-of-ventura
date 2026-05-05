
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.client.screens.*
import org.joml.Vector4f
import org.joml.Vector4fc
import java.io.File

private val FILE_TREE_BACKGROUND: Vector4fc
    = Vector4f(0.2f, 0.2f, 0.2f, 0.35f)

fun multiFileEditorScreen(client: Client): () -> GameScreen = {
    val contextMenu = ContextMenu()
    val editorCont = Stack()
    var editor: CodeEditor? = null
    fun openFile(file: File) {
        val newEditor = CodeEditor.openFile(file) ?: return
        editorCont.disposeAll()
        editorCont.add(newEditor.root)
        editor = newEditor
    }
    fun closeFile() {
        editor?.save()
        editorCont.disposeAll()
        editor = null
    }
    val screen = PausedScreen(
        client,
        camMode = { w -> CameraModes.playerFarCentered(w.player) },
        closeIf = { Key.ESCAPE.wasPressed || Key.C.wasPressed },
        playerFollowCursor = false,
        playerAnim = PersonAnim.thinking,
        render = {
            editor?.update()
        },
        onClose = {
            editor?.save()
        }
    )
    screen.screen.add(layer = 0, element = Stack()
        .add(screen.background)
        .add(FlatBackground().withColor(FILE_TREE_BACKGROUND))
        .add(Axis.row()
            .add(1f/3f * fpw, Stack()
                .add(FlatBackground().withColor(FILE_TREE_BACKGROUND))
                .add(createFileTree(FileTreeCtx(
                    contextMenu,
                    onFileSelect = { openFile(it) },
                    isMutable = true,
                    fileExt = "bigton",
                    onFileDelete = { file ->
                        if (editor?.file?.canonicalPath == file.canonicalPath) {
                            closeFile()
                        }
                    },
                    onFileRename = { from, to ->
                        if (editor?.file?.canonicalPath == from.canonicalPath) {
                            closeFile()
                            openFile(to)
                        }
                    }
                )))
            )
            .add(2f/3f * fpw, Stack()
                .add(FlatBackground().withColor(CodeEditor.BACKGROUND_COLOR))
                .add(editorCont)
            )
        )
    )
    screen.screen.add(layer = 1, element = contextMenu)
    screen.screen
}
