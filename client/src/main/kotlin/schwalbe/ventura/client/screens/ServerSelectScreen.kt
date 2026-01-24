
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*

private fun createServerOption(
    server: Config.Server, i: Int, client: Client
): UiElement = Axis.row()
    .add(80.pw, createButton(
        content = Axis.column()
            .add(60.ph, Text()
                .withFont(googleSansSb())
                .withText(server.name)
                .withSize(70.ph)
            )
            .add(40.ph, Text()
                .withFont(jetbrainsMonoSb())
                .withText("${server.address}:${server.port}")
                .withSize(70.ph)
                .withColor(SECONDARY_FONT_COLOR)
            )
            .pad(1.5.vmin),
        handler = {
            // TODO! connect to server (logic in 'Client')
            client.nav.push(serverConnectingScreen(
                name = "${server.address}:${server.port}",
                client
            ))
        }
    ))
    .add(20.pw - 5.vmin, Axis.column()
        .add(50.ph - 0.5.vmin, createTextButton(
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
        .add(1.vmin, Stack())
        .add(50.ph - 0.5.vmin, createTextButton(
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
        .add(50.ph - 0.5.vmin, createTextButton(
            content = "↑",
            handler = {
                val servers = client.config.servers
                val moved = servers.removeAt(i)
                val destI: Int = if (i >= 1) { i - 1 } else { servers.size }
                servers.add(destI, moved)
                client.config.write()
                client.nav.replace(serverSelectScreen(client))
            }
        ))
        .add(1.vmin, Stack())
        .add(50.ph - 0.5.vmin, createTextButton(
            content = "↓",
            handler = {
                val servers = client.config.servers
                val moved = servers.removeAt(i)
                val destI: Int = if (i + 1 <= servers.size) { i + 1 } else { 0 }
                servers.add(destI, moved)
                client.config.write()
                client.nav.replace(serverSelectScreen(client))
            }
        ))
        .pad(left = 1.vmin)
    )
    .pad(bottom = 1.5.vmin)

fun serverSelectScreen(client: Client): UiScreenDef = defineScreen(
    defaultFontColor = BASE_FONT_COLOR
) {
    client.onFrame = renderGridBackground(client)
    val serverList = Axis.column()
    for ((i, server) in client.config.servers.withIndex()) {
        serverList.add(9.5.vmin, createServerOption(server, i, client))
    }
    if (client.config.servers.isEmpty()) {
        serverList.add(5.vmin, Text()
            .withText(localized()[PLACEHOLDER_NO_SERVERS])
            .withSize(2.vmin)
            .withColor(SECONDARY_FONT_COLOR)
        )
    }
    it.add(layer = 0, element = Axis.column()
        .add(7.ph, Axis.row()
            .add(100.pw - 30.vmin, Text()
                .withFont(googleSansSb())
                .withText(localized()[TITLE_SELECT_SERVER])
                .withSize(2.5.vmin)
            )
            .add(30.vmin, createTextButton(
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
        .add(5.vmin, createTextButton(
            content = localized()[BUTTON_GO_BACK],
            handler = {
                client.nav.pop()
            }
        ).pad(top = 1.vmin, right = 100.pw - 30.vmin))
        .pad(5.vmin)
    )
}
