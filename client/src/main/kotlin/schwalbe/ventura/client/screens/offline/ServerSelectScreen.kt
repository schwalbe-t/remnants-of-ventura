
package schwalbe.ventura.client.screens.offline

import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*

private fun createServerOption(
    server: Config.Server, i: Int,
    addrName: String, loggedUsername: String?,
    client: Client
): UiElement = Axis.row()
    .add(80.pw, _root_ide_package_.schwalbe.ventura.client.screens.createButton(
        content = Axis.column()
            .add(
                60.ph, Text()
                    .withFont(_root_ide_package_.schwalbe.ventura.client.screens.googleSansSb())
                    .withText(server.name)
                    .withSize(70.ph)
            )
            .add(
                40.ph, Text()
                    .withText(
                        listOf(
                            Span(
                                text = addrName,
                                font = _root_ide_package_.schwalbe.ventura.client.screens.jetbrainsMonoSb()
                            ),
                            Span(
                                text = if (loggedUsername == null) {
                                    ""
                                } else {
                                    "    ${localized()[LABEL_LOGGED_IN_AS]} "
                                }
                            ),
                            Span(
                                text = loggedUsername ?: "",
                                font = _root_ide_package_.schwalbe.ventura.client.screens.googleSansSb()
                            )
                        )
                    )
                    .withSize(70.ph)
                    .withColor(_root_ide_package_.schwalbe.ventura.client.screens.SECONDARY_FONT_COLOR)
            )
            .pad(1.5.vmin),
        handler = {
            client.nav.push(
                serverConnectingScreen(
                    name = "${server.address}:${server.port}",
                    client
                )
            )
            client.network.connect(
                server.address, server.port
            )
        }
    ))
    .add(20.pw - 5.vmin, Axis.column()
        .add(50.ph - 0.5.vmin,
            _root_ide_package_.schwalbe.ventura.client.screens.createTextButton(
                content = localized()[BUTTON_EDIT_SERVER],
                handler = {
                    val edited = client.config.servers[i]
                    client.nav.push(serverEditScreen(edited, client) { r ->
                        client.config.servers[i] = r
                        client.config.write()
                        client.nav.replace(serverSelectScreen(client))
                    })
                }
            ))
        .add(1.vmin, Space())
        .add(50.ph - 0.5.vmin,
            _root_ide_package_.schwalbe.ventura.client.screens.createTextButton(
                content = localized()[BUTTON_DELETE_SERVER],
                handler = {
                    client.config.servers.removeAt(i)
                    client.config.write()
                    client.nav.replace(serverSelectScreen(client))
                }
            ))
        .pad(left = 1.vmin)
    )
    .add(5.vmin, Axis.column()
        .add(50.ph - 0.5.vmin,
            _root_ide_package_.schwalbe.ventura.client.screens.createTextButton(
                content = "↑",
                handler = {
                    val servers = client.config.servers
                    val moved = servers.removeAt(i)
                    val destI: Int = if (i >= 1) {
                        i - 1
                    } else {
                        servers.size
                    }
                    servers.add(destI, moved)
                    client.config.write()
                    client.nav.replace(serverSelectScreen(client))
                }
            ))
        .add(1.vmin, Space())
        .add(50.ph - 0.5.vmin,
            _root_ide_package_.schwalbe.ventura.client.screens.createTextButton(
                content = "↓",
                handler = {
                    val servers = client.config.servers
                    val moved = servers.removeAt(i)
                    val destI: Int = if (i + 1 <= servers.size) {
                        i + 1
                    } else {
                        0
                    }
                    servers.add(destI, moved)
                    client.config.write()
                    client.nav.replace(serverSelectScreen(client))
                }
            ))
        .pad(left = 1.vmin)
    )
    .pad(bottom = 1.5.vmin)

fun serverSelectScreen(client: Client): schwalbe.ventura.client.screens.GameScreen {
    val screen = _root_ide_package_.schwalbe.ventura.client.screens.GameScreen(
        render = _root_ide_package_.schwalbe.ventura.client.screens.renderGridBackground(
            client
        ),
        networkState = _root_ide_package_.schwalbe.ventura.client.screens.noNetworkConnections(
            client
        ),
        navigator = client.nav
    )
    val serverList = Axis.column()
    for ((i, server) in client.config.servers.withIndex()) {
        val addrName = "${server.address}:${server.port}"
        val loggedUsername = client.config.sessions[addrName]?.username
        serverList.add(9.5.vmin, createServerOption(
            server, i, addrName, loggedUsername, client
        ))
    }
    if (client.config.servers.isEmpty()) {
        serverList.add(5.vmin, Text()
            .withText(localized()[PLACEHOLDER_NO_SERVERS])
            .withSize(2.vmin)
            .withColor(_root_ide_package_.schwalbe.ventura.client.screens.SECONDARY_FONT_COLOR)
        )
    }
    screen.add(layer = 0, element = Axis.column()
        .add(7.ph, Axis.row()
            .add(100.pw - 30.vmin, Text()
                .withFont(_root_ide_package_.schwalbe.ventura.client.screens.googleSansSb())
                .withText(localized()[TITLE_SELECT_SERVER])
                .withSize(2.5.vmin)
            )
            .add(30.vmin,
                _root_ide_package_.schwalbe.ventura.client.screens.createTextButton(
                    content = localized()[BUTTON_ADD_SERVER],
                    handler = {
                        client.nav.push(serverEditScreen(null, client) { r ->
                            client.config.servers.add(r)
                            client.config.write()
                            client.nav.replace(serverSelectScreen(client))
                        })
                    }
                ))
            .pad(bottom = 2.vmin)
        )
        .add(93.ph - 5.vmin, serverList
            .wrapScrolling()
        )
        .add(5.vmin, _root_ide_package_.schwalbe.ventura.client.screens.createTextButton(
            content = localized()[BUTTON_GO_BACK],
            handler = {
                client.nav.pop()
            }
        ).pad(top = 1.vmin, right = 100.pw - 30.vmin))
        .pad(5.vmin)
    )
    return screen
}
