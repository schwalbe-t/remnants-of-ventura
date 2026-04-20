
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.LocalKeys
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.game.USERCODE_DIR
import schwalbe.ventura.client.localized
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.engine.ui.*
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import org.joml.Vector4f
import org.joml.Vector4fc
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.nio.file.Path
import java.nio.file.StandardCopyOption

private val ILLEGAL_FILE_NAME_CHARS: Set<Char> = setOf(
    '/', '\n', '\r', '\t', '\u0000', '\u000C', '`', '?', '*', '\\', '<', '>',
    '|', '\"', ':'
)

private val FILE_MENU_BORDER_RADIUS: UiSize = 0.75.vmin
private val FILE_MENU_BACKGROUND: Vector4fc
    = Vector4f(0.2f, 0.2f, 0.2f, 0.95f)
private val FILE_MENU_BUTTON: Vector4fc
    = Vector4f(0.7f, 0.7f, 0.7f, 0.2f)
private val FILE_MENU_HOVER: Vector4fc
    = Vector4f(0.7f, 0.7f, 0.7f, 0.4f)

private val FILE_NAME_MENU_WIDTH: UiSize = 40.vmin
private val FILE_NAME_INVALID_COLOR: Vector4fc
    = Vector4f(0.9f, 0.5f, 0.5f, 1f)

private fun createFileNameButton(text: Text, onClick: () -> Unit) = Stack()
    .add(FlatBackground()
        .withColor(0, 0, 0, 0)
        .withHoverColor(FILE_MENU_HOVER)
    )
    .add(text
        .alignCenter()
        .withFont(googleSansSb())
        .withSize(85.ph)
        .pad(0.35.vmin)
    )
    .add(ClickArea().withLeftHandler(onClick))
    .wrapBorderRadius(0.5.vmin)

private fun createFileNameMenu(
    parentDir: File,
    actionKey: LocalKeys,
    initialValue: String,
    onNameChosen: (String) -> Unit,
    close: () -> Unit
): UiElement {
    val l = localized()
    val inputText = Text()
        .withFont(googleSansR())
        .withSize(1.65.vmin)
    val cancelText = Text().withText(l[BUTTON_CANCEL_FILE_ACTION])
    val confirmText = Text().withText(l[actionKey])
    var inputValid = true
    val input = TextInput()
        .withContent(inputText)
        .withValue(initialValue)
        .withValueChangedHandler { newValue ->
            val exists = parentDir.resolve(newValue).exists()
            inputValid = newValue.isNotEmpty()
                && newValue.all { it !in ILLEGAL_FILE_NAME_CHARS }
                && (!exists || newValue == initialValue)
            inputText.withColor(
                if (inputValid) Theme.FONT_COLOR
                else FILE_NAME_INVALID_COLOR
            )
            confirmText.withColor(
                if (inputValid) Theme.FONT_COLOR
                else FILE_MENU_BUTTON
            )
        }
    val titleH: UiSize = 5.vmin
    val inputH: UiSize = 5.vmin
    val buttonW: UiSize = 15.vmin
    val buttonH: UiSize = 4.5.vmin
    return Stack()
        .add(FlatBackground()
            .withColor(FILE_MENU_BACKGROUND)
            .withHoverColor(FILE_MENU_BACKGROUND)
        )
        .add(Axis.column()
            .add(titleH, Text()
                .withText(localized()[TITLE_ENTER_FILE_NAME])
                .withFont(googleSansSb())
                .withSize(100.ph)
                .pad(1.5.vmin)
            )
            .add(inputH, Stack()
                .add(FlatBackground()
                    .withColor(FILE_MENU_BUTTON)
                    .withHoverColor(FILE_MENU_HOVER)
                    .wrapBorderRadius(0.5.vmin)
                )
                .add(input
                    .pad(0.5.vmin)
                )
                .wrapBorderRadius(0.5.vmin)
                .pad(1.vmin)
            )
            .add(buttonH, Axis.row()
                .add(100.pw - buttonW - 1.vmin - buttonW, Space())
                .add(buttonW, createFileNameButton(cancelText) {
                    close()
                })
                .add(1.vmin, Space())
                .add(buttonW, createFileNameButton(confirmText) {
                    if (!inputValid) { return@createFileNameButton }
                    onNameChosen(input.valueString)
                    close()
                })
                .pad(1.vmin)
            )
        )
        .wrapBorderRadius(FILE_MENU_BORDER_RADIUS)
        .withWidth(FILE_NAME_MENU_WIDTH)
        .withHeight(titleH + inputH + buttonH)
}

private val FILE_CTX_MENU_WIDTH: UiSize = 40.vmin
private val FILE_CTX_MENU_ENTRY_HEIGHT: UiSize = 3.vmin

private fun createContextMenuItem(text: String, font: Font) = Text()
    .withText(text)
    .withSize(75.ph)
    .withFont(font)
    .pad(0.5.vmin)

private fun createContextMenuAction(
    text: String, action: () -> Unit
): UiElement = Stack()
    .add(FlatBackground()
        .withColor(0, 0, 0, 0)
        .withHoverColor(FILE_MENU_HOVER)
        .wrapBorderRadius(0.5.vmin)
    )
    .add(createContextMenuItem(text, googleSansSb()))
    .add(ClickArea().withLeftHandler(action))

private fun File.isEditorDeletable(): Boolean =
    if (!this.isDirectory) { true }
    else Files.list(this.toPath()).use { it.findAny().isEmpty }

private fun createFileContextMenu(
    file: File, ctx: FileTreeCtx, isRoot: Boolean
): UiElement? {
    val l = localized()
    val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val actionList: SizedAxis = Axis.column(FILE_CTX_MENU_ENTRY_HEIGHT)
    if (ctx.isMutable && file.isDirectory) {
        actionList.add(createContextMenuAction(l[BUTTON_CREATE_FILE]) {
            ctx.contextMenu.open(createFileNameMenu(
                file, BUTTON_CREATE_FILE, initialValue = "",
                onNameChosen = h@{ newName ->
                    val dest = file.resolve(newName)
                    Files.writeString(dest.toPath(), "")
                    ctx.updateTree()
                },
                close = { ctx.contextMenu.close() }
            ))
        })
        actionList.add(createContextMenuAction(l[BUTTON_CREATE_FOLDER]) {
            ctx.contextMenu.open(createFileNameMenu(
                file, BUTTON_CREATE_FOLDER, initialValue = "",
                onNameChosen = h@{ newName ->
                    val dest = file.resolve(newName)
                    Files.createDirectory(dest.toPath())
                    ctx.updateTree()
                },
                close = { ctx.contextMenu.close() }
            ))
        })
    }
    if (ctx.isMutable && !isRoot) {
        actionList.add(createContextMenuAction(l[BUTTON_RENAME_FILE]) {
            ctx.contextMenu.open(createFileNameMenu(
                file.parentFile, BUTTON_RENAME_FILE, initialValue = file.name,
                onNameChosen = h@{ newName ->
                    if (newName == file.name) { return@h }
                    val from: Path = file.toPath()
                    val to: Path = from.resolveSibling(newName)
                    Files.move(from, to)
                    ctx.onFileRename(file, to.toFile())
                    ctx.updateTree()
                },
                close = { ctx.contextMenu.close() }
            ))
        })
        actionList.add(createContextMenuAction(l[BUTTON_COPY_FILE]) {
            val files: List<File> = listOf(file)
            val transferable = object : Transferable {
                override fun getTransferDataFlavors(): Array<DataFlavor>
                    = arrayOf(DataFlavor.javaFileListFlavor)
                override fun isDataFlavorSupported(f: DataFlavor?): Boolean
                    = f == DataFlavor.javaFileListFlavor
                override fun getTransferData(f: DataFlavor?): Any
                    = files
            }
            clipboard.setContents(transferable, null)
            ctx.contextMenu.close()
        })
    }
    if (ctx.isMutable && file.isDirectory) {
        actionList.add(createContextMenuAction(l[BUTTON_PASTE_FILES]) h@{
            val copied: Transferable = clipboard.getContents(null) ?: return@h
            if (!copied.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return@h
            }
            val copiedFilesRaw: List<*> = copied
                .getTransferData(DataFlavor.javaFileListFlavor)
                as? List<*> ?: return@h
            val copiedFiles = copiedFilesRaw.filterIsInstance<File>()
            for (copiedFile in copiedFiles) {
                val src: Path = copiedFile.toPath()
                val dest: Path = file.toPath().resolve(copiedFile.name)
                val copyOpt = StandardCopyOption.REPLACE_EXISTING
                try {
                    if (copiedFile.isDirectory) {
                        Files.walk(src).toList().forEach { chSrc ->
                            val chDest = dest.resolve(src.relativize(chSrc))
                            if (Files.isDirectory(chDest)) {
                                Files.createDirectories(chDest)
                            } else {
                                Files.copy(chSrc, chDest, copyOpt)
                            }
                        }
                    } else {
                        Files.copy(src, dest, copyOpt)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            ctx.updateTree()
            ctx.contextMenu.close()
        })
    }
    if (ctx.isMutable && !isRoot && file.isEditorDeletable()) {
        actionList.add(createContextMenuAction(l[BUTTON_DELETE_FILE]) {
            file.delete()
            ctx.onFileDelete(file)
            ctx.updateTree()
            ctx.contextMenu.close()
        })
    }
    if (file.isDirectory) {
        actionList.add(createContextMenuAction(l[BUTTON_OPEN_IN_FILE_EXPLORER]) {
            try {
                Desktop.getDesktop().open(file)
            } catch(e: Exception) {
                e.printStackTrace()
            }
            ctx.contextMenu.close()
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
            .withColor(FILE_MENU_BACKGROUND)
            // blocks elements below menu from displaying hover color
            .withHoverColor(FILE_MENU_BACKGROUND)
        )
        .add(Axis.column()
            .add(FILE_CTX_MENU_ENTRY_HEIGHT,
                createContextMenuItem(file.name, googleSansR())
            )
            .add(actionsH, actionList)
            .pad(p)
        )
        .wrapBorderRadius(FILE_MENU_BORDER_RADIUS)
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
    .withSize(75.ph)

private fun createFileEntry(
    file: File, ctx: FileTreeCtx
): UiElement = Axis.row()
    .add(FILE_INDENT, Space())
    .add(100.pw - FILE_INDENT, Stack()
        .add(FlatBackground()
            .withColor(0, 0, 0, 0)
            .withHoverColor(Theme.BUTTON_HOVER_COLOR)
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
): Pair<UiSize, UiElement> {
    val rawDirContents: Array<File> = folder.listFiles() ?: arrayOf()
    val containedFolders: List<File> = rawDirContents
        .filter { it.isDirectory }.sorted()
    val containedFiles: List<File> = rawDirContents
        .filter { !it.isDirectory }.sorted()
    val dirContents = containedFolders + containedFiles
    var resultHeight: UiSize = FILE_HEIGHT
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
            val (subHeight, tree) = createFolderSubTree(inner, ctx)
            items.add(subHeight, tree)
            resultHeight += subHeight
        } else {
            val entry = createFileEntry(inner, ctx)
            items.add(FILE_HEIGHT, entry)
            resultHeight += FILE_HEIGHT
        }
    }
    val tree = Axis.row()
        .add(FILE_INDENT, Text()
            .withText("⌄")
            .alignRight()
            .withFont(jetbrainsMonoB())
            .withColor(Theme.SECONDARY_FONT_COLOR)
            .withSize(60.ph)
            .pad(FILE_PADDING)
            .withHeight(FILE_HEIGHT)
        )
        .add(100.pw - FILE_INDENT, items)
    return resultHeight to tree
}

private val FILE_TREE_BOTTOM_PADDING: UiSize = 50.ph

fun createFileTree(ctx: FileTreeCtx): UiElement {
    val rootDir = File(USERCODE_DIR)
    val container = Stack()
    val p: UiSize = 1.5.vmin
    val paddedContainer = container.pad(p)
    ctx.updateTree = {
        val (height, tree) = createFolderSubTree(rootDir, ctx, isRoot = true)
        container.disposeAll()
        container.add(tree)
        paddedContainer.withHeight(height + FILE_TREE_BOTTOM_PADDING + 2 * p)
    }
    ctx.updateTree()
    return paddedContainer
        .wrapThemedScrolling(vert = true, horiz = true)
        .pad(0.5.vmin)
}