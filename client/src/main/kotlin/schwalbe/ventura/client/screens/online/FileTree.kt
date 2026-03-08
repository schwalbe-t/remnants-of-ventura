
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.LocalKeys
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.game.USERCODE_DIR
import schwalbe.ventura.client.localized
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.engine.ui.*
import java.awt.Desktop
import java.io.File
import org.joml.Vector4f
import org.joml.Vector4fc
import java.nio.file.Files

private val FILE_CTX_MENU_WIDTH: UiSize = 40.vmin
private val FILE_CTX_MENU_ENTRY_HEIGHT: UiSize = 3.vmin
private val FILE_CTX_MENU_BACKGROUND: Vector4fc
    = Vector4f(0.2f, 0.2f, 0.2f, 0.85f)
private val FILE_CTX_MENU_ACTION_HOVER: Vector4fc
    = Vector4f(0.7f, 0.7f, 0.7f, 0.4f)

private fun createContextMenuItem(text: String, font: Font) = Text()
    .withText(text)
    .withSize(75.ph)
    .withFont(font)
    .withColor(BRIGHT_FONT_COLOR)
    .pad(0.5.vmin)

private fun createContextMenuAction(
    localKey: LocalKeys, action: () -> Unit
): UiElement = Stack()
    .add(FlatBackground()
        .withColor(0, 0, 0, 0)
        .withHoverColor(FILE_CTX_MENU_ACTION_HOVER)
        .wrapBorderRadius(0.5.vmin)
    )
    .add(createContextMenuItem(localized()[localKey], googleSansSb()))
    .add(ClickArea().withLeftHandler(action))

private fun File.isEditorDeletable(): Boolean =
    if (!this.isDirectory) { true }
    else Files.list(this.toPath()).use { it.findAny().isEmpty }

private fun createFileContextMenu(
    file: File, ctx: FileTreeCtx, isRoot: Boolean
): UiElement? {
    val actionList: SizedAxis = Axis.column(FILE_CTX_MENU_ENTRY_HEIGHT)
    if (file.isDirectory && ctx.isMutable) {
        actionList.add(createContextMenuAction(BUTTON_CREATE_FILE) {
            // TODO!
        })
        actionList.add(createContextMenuAction(BUTTON_CREATE_FOLDER) {
            // TODO!
        })
    }
    if (ctx.isMutable && !isRoot) {
        // TODO: cut, copy, paste
        actionList.add(createContextMenuAction(BUTTON_RENAME_FILE) {
            // TODO!
            // NOTE: call ctx.onFileRename(from, to)
        })
    }
    if (ctx.isMutable && !isRoot && file.isEditorDeletable()) {
        actionList.add(createContextMenuAction(BUTTON_DELETE_FILE) {
            file.delete()
            ctx.onFileDelete(file)
            ctx.updateTree()
        })
    }
    if (file.isDirectory) {
        actionList.add(createContextMenuAction(BUTTON_OPEN_IN_FILE_EXPLORER) {
            try {
                Desktop.getDesktop().open(file)
            } catch(e: Exception) {
                e.printStackTrace()
            }
        })
    }
    if (actionList.children.isEmpty()) {
        actionList.disposeTree()
        return null
    }
    val actionsH: UiSize = FILE_CTX_MENU_ENTRY_HEIGHT * actionList.children.size
    val p: UiSize = 0.75.vmin
    return Stack()
        .add(FlatBackground()
            .withColor(FILE_CTX_MENU_BACKGROUND)
            // blocks elements below menu from displaying hover color
            .withHoverColor(FILE_CTX_MENU_BACKGROUND)
        )
        .add(Axis.column()
            .add(FILE_CTX_MENU_ENTRY_HEIGHT,
                createContextMenuItem(file.name, googleSansR())
            )
            .add(actionsH, actionList)
            .pad(p)
        )
        .wrapBorderRadius(0.75.vmin)
        .withWidth(FILE_CTX_MENU_WIDTH)
        .withHeight(
            actionsH + FILE_CTX_MENU_ENTRY_HEIGHT + 2 * p
        )
}

class FileTreeCtx(
    val contextMenu: ContextMenu,
    val onFileSelect: (File) -> Unit,
    val isMutable: Boolean = false,
    val onFileRename: (File, File) -> Unit = { _, _ -> },
    val onFileDelete: (File) -> Unit = {}
) {
    var updateTree: () -> Unit = {}
}

val FILE_INDENT: UiSize = 2.vmin
val FILE_HEIGHT: UiSize = 3.vmin
val FILE_PADDING: UiSize = 0.5.vmin

private fun createFileText(text: String) = Text()
    .withText(text)
    .withFont(googleSansR())
    .withColor(BRIGHT_FONT_COLOR)
    .withSize(75.ph)

private fun createFileEntry(
    file: File, ctx: FileTreeCtx
): UiElement = Axis.row()
    .add(FILE_INDENT, Space())
    .add(100.pw - FILE_INDENT, Stack()
        .add(FlatBackground()
            .withColor(0, 0, 0, 0)
            .withHoverColor(BUTTON_HOVER_COLOR)
            .wrapBorderRadius(0.5.vmin)
        )
        .add(createFileText(file.name)
            .pad(FILE_PADDING)
        )
        .add(ClickArea()
            .withLeftHandler { ctx.onFileSelect(file) }
            .withRightHandler {
                val menu = createFileContextMenu(file, ctx, isRoot = false)
                    ?: return@withRightHandler
                ctx.contextMenu.open(menu)
            }
        )
    )

private fun createFolderSubTree(
    folder: File, ctx: FileTreeCtx, isRoot: Boolean = false
): Pair<Int, UiElement> {
    val rawDirContents: Array<File> = folder.listFiles() ?: arrayOf()
    val containedFolders: List<File> = rawDirContents
        .filter { it.isDirectory }.sorted()
    val containedFiles: List<File> = rawDirContents
        .filter { !it.isDirectory }.sorted()
    val dirContents = containedFolders + containedFiles
    var numFiles: Int = 1
    val items = Axis.column()
    items.add(FILE_HEIGHT, Stack()
        .add(createFileText(folder.name).pad(FILE_PADDING))
        .add(ClickArea().withRightHandler {
            val menu = createFileContextMenu(folder, ctx, isRoot)
                ?: return@withRightHandler
            ctx.contextMenu.open(menu)
        })
    )
    for (inner in dirContents) {
        if (inner.isDirectory) {
            val (numSubFiles, tree) = createFolderSubTree(inner, ctx)
            items.add(numSubFiles * FILE_HEIGHT, tree)
            numFiles += numSubFiles
        } else {
            val entry = createFileEntry(inner, ctx)
            items.add(FILE_HEIGHT, entry)
            numFiles += 1
        }
    }
    val tree = Axis.row()
        .add(FILE_INDENT, Text()
            .withText("⌄")
            .alignRight()
            .withFont(jetbrainsMonoB())
            .withColor(SECONDARY_BRIGHT_FONT_COLOR)
            .withSize(60.ph)
            .pad(FILE_PADDING)
            .withHeight(FILE_HEIGHT)
        )
        .add(100.pw - FILE_INDENT, items)
    return numFiles to tree
}

fun createFileTree(ctx: FileTreeCtx): UiElement {
    val rootDir = File(USERCODE_DIR)
    val container = Stack()
    ctx.updateTree = {
        container.disposeAll()
        container.add(createFolderSubTree(rootDir, ctx, isRoot = true).second)
    }
    ctx.updateTree()
    return container
        .pad(1.5.vmin)
        .wrapScrolling(vert = true, horiz = true)
        .withThumbColor(BUTTON_COLOR)
        .withThumbHoverColor(BUTTON_HOVER_COLOR)
        .pad(0.5.vmin)
}