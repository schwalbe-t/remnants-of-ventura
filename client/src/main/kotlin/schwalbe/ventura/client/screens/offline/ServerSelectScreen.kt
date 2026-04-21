
package schwalbe.ventura.client.screens.offline

import org.joml.Vector4f
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.screens.*

private val NAV_BAR_HEIGHT: UiSize = 5.vmin

private fun createNavbarAction(icon: Icon, action: () -> Unit) = Stack()
    .add(FlatBackground()
        .withColor(Vector4f(0f, 0f, 0f, 0f))
        .withHoverColor(Theme.BUTTON_HOVER_COLOR)
    )
    .add(icon.create(2.vmin)
        .pad(horizontal = 4.vmin, vertical = 1.5.vmin)
    )
    .add(ClickArea().withLeftHandler(action))

private val SERVER_ACTION_SIZE: UiSize = floor(5.vmin)

private class ServerAction(
    val icon: Icon?,
    val iconPad: UiSize = 0.25.vmin,
    val content: UiElement? = null,
    val action: () -> Unit = {}
)

private fun createServerListItem(
    actions: Iterable<ServerAction>
): UiElement {
    val container = Axis.row()
    var currContW: UiSize = 0.px
    for (action in actions) {
        if (container.children.isNotEmpty()) {
            container.add(1.vmin, Space())
            currContW += 1.vmin
        }
        val buttonW: UiSize = if (action.content == null) 4.vmin
            else 100.pw - currContW
        val icon: Icon? = action.icon
        if (icon != null) {
            val contents = Axis.row()
            val iconSize: UiSize = 3.vmin - (2 * action.iconPad)
            contents.add(3.vmin, icon.create(iconSize).pad(action.iconPad))
            action.content?.let { c ->
                contents.add(0.5.vmin, Space())
                contents.add(100.pw - 0.5.vmin - 3.vmin, c)
            }
            container.add(buttonW, Theme.button(
                content = contents.pad(0.5.vmin),
                handler = action.action
            ))
        } else {
            container.add(buttonW, Space())
        }
        currContW += buttonW
    }
    return container.pad(top = 0.5.vmin, bottom = 0.5.vmin, right = 1.vmin)
}

fun serverSelectScreen(client: Client): () -> GameScreen = {
    val root = Stack()
    val background = WorldBackground(backgroundWorld(), client)
    val screen = GameScreen(
        render = {
            background.render()
            root.invalidate()
        },
        networkState = noNetworkConnections(client),
        navigator = client.nav,
        onClose = background::dispose
    )
    val serverList = Axis.column(SERVER_ACTION_SIZE)
    for ((i, server) in client.config.servers.withIndex()) {
        val addrName = if (server.port == DEFAULT_SERVER_PORT) server.address
            else "${server.address}:${server.port}"
        serverList.add(createServerListItem(listOf(
            ServerAction(
                icon = Icons.DELETE,
                iconPad = 0.5.vmin,
                action = {
                    client.config.servers.removeAt(i)
                    client.config.write()
                    client.nav.replace(serverSelectScreen(client))
                }
            ),
            ServerAction(
                icon = Icons.EDIT,
                iconPad = 0.5.vmin,
                action = {
                    val edited = client.config.servers[i]
                    client.nav.push(serverEditScreen(edited, client) { r ->
                        client.config.servers[i] = r
                        client.config.write()
                        client.nav.replace(serverSelectScreen(client))
                    })
                }
            ),
            ServerAction(
                icon = Icons.PLAY,
                content = Axis.row()
                    .add(60.pw, Text()
                        .withText(server.name)
                        .withFont(googleSansSb())
                        .withSize(80.ph)
                        .pad(horizontal = 0.px, vertical = 0.5.vmin)
                    )
                    .add(40.pw, Text()
                        .withText(addrName)
                        .withFont(jetbrainsMonoSb())
                        .withSize(80.ph)
                        .withColor(Theme.SECONDARY_FONT_COLOR)
                        .alignRight()
                        .pad(top = 0.75.vmin, bottom = 0.75.vmin, right = 0.5.vmin)
                    ),
                action = {
                    client.nav.push(serverConnectingScreen(
                        server.address, server.port, client
                    ))
                }
            )
        )))
    }
    serverList.add(createServerListItem(listOf(
        ServerAction(null),
        ServerAction(null),
        ServerAction(
            icon = Icons.ADD,
            content = Text().withText(localized()[BUTTON_ADD_SERVER])
                .withSize(80.ph)
                .pad(horizontal = 0.px, vertical = 0.5.vmin),
            action = {
                client.nav.push(serverEditScreen(null, client) { r ->
                    client.config.servers.add(r)
                    client.config.write()
                    client.nav.replace(serverSelectScreen(client))
                })
            }
        )
    )))
    val serverListH: UiSize = serverList.children.size * SERVER_ACTION_SIZE
    val serverListP: UiSize = floor(maxOf((100.ph - serverListH) / 2, 5.vmin))
    root.add(Axis.column()
        .add(NAV_BAR_HEIGHT, Stack()
            .add(BlurBackground()
                .withRadius(5)
            )
            .add(FlatBackground().withColor(Theme.BUTTON_COLOR))
            .add(Axis.row(10.vmin)
                .add(createNavbarAction(Icons.SETTINGS) {

                })
                .add(createNavbarAction(Icons.TRANSLATE) {
                    client.nav.push(languageSelectScreen(client))
                })
                .add(createNavbarAction(Icons.POWER) {
                    client.window.close()
                })
            )
        )
        .add(100.ph - NAV_BAR_HEIGHT, Axis.row()
            .add(50.pw + 1.vmin, Axis.column()
                .add(serverListP, Space())
                .add(serverListH, serverList)
                .add(serverListP, Space())
                .wrapThemedScrolling(horiz = false, vert = true)
                .pad(left = 1.vmin)
            )
        )
    )
    screen.add(layer = 0, element = root)
    screen
}