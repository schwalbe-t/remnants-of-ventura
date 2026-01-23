
package schwalbe.ventura.client.screens

import schwalbe.ventura.client.Config
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.localized
import schwalbe.ventura.client.write
import schwalbe.ventura.engine.ui.*

private fun createServerOption(
    server: Config.Server, i: Int, config: Config, nav: UiNavigator
): UiElement = Axis.row()
    .add(80.pw, createButton(
        content = Axis.column()
            .add(60.ph, Text()
                .withFont(jetbrainsMonoB())
                .withText(server.name)
                .withSize(70.ph)
            )
            .add(40.ph, Text()
                .withText("${server.address}:${server.port}")
                .withSize(70.ph)
                .withColor(DARK_FONT_COLOR)
            )
            .pad(1.vmin),
        handler = {
            // TODO!
            println("CONNECT TO '${server.address}:${server.port}'")
        }
    ))
    .add(20.pw - 5.vmin, Axis.column()
        .add(50.ph - 0.5.vmin, createTextButton(
            content = localized()[BUTTON_EDIT_SERVER],
            handler = {
                val edited = config.servers[i]
                nav.clear(serverEditScreen(edited, config, nav) { result ->
                    config.servers[i] = result
                    config.write()
                })
            }
        ))
        .add(1.vmin, Stack())
        .add(50.ph - 0.5.vmin, createTextButton(
            content = localized()[BUTTON_DELETE_SERVER],
            handler = {
                config.servers.removeAt(i)
                config.write()
                nav.replace(serverSelectScreen(config, nav))
            }
        ))
        .pad(left = 1.vmin)
    )
    .add(5.vmin, Axis.column()
        .add(50.ph - 0.5.vmin, createTextButton(
            content = "↑",
            handler = {
                val moved = config.servers.removeAt(i)
                val destI: Int = if (i >= 1) { i - 1 }
                    else { config.servers.size }
                config.servers.add(destI, moved)
                config.write()
                nav.replace(serverSelectScreen(config, nav))
            }
        ))
        .add(1.vmin, Stack())
        .add(50.ph - 0.5.vmin, createTextButton(
            content = "↓",
            handler = {
                val moved = config.servers.removeAt(i)
                val destI: Int = if (i + 1 <= config.servers.size) { i + 1 }
                    else { 0 }
                config.servers.add(destI, moved)
                config.write()
                nav.replace(serverSelectScreen(config, nav))
            }
        ))
        .pad(left = 1.vmin)
    )
    .pad(bottom = 1.5.vmin)

fun serverSelectScreen(config: Config, nav: UiNavigator): UiScreenDef
= defineScreen(
    defaultFontColor = BASE_FONT_COLOR
) {
    val serverList = Axis.column()
    for ((i, server) in config.servers.withIndex()) {
        serverList.add(9.5.vmin, createServerOption(server, i, config, nav))
    }
    if (config.servers.isEmpty()) {
        serverList.add(5.vmin, Text()
            .withText(localized()[PLACEHOLDER_NO_SERVERS])
            .withSize(2.vmin)
            .withColor(DARK_FONT_COLOR)
        )
    }
    it.add(layer = -1, element = FlatBackground()
        .withColor(BACKGROUND_COLOR)
    )
    it.add(layer = 0, element = Axis.column()
        .add(7.ph, Axis.row()
            .add(85.pw, Text()
                .withText(localized()[TITLE_SELECT_SERVER])
                .withSize(2.5.vmin)
            )
            .add(15.pw, createTextButton(
                content = localized()[BUTTON_ADD_SERVER],
                handler = {
                    nav.replace(serverEditScreen(null, config, nav) { result ->
                        config.servers.add(result)
                        config.write()
                    })
                }
            ))
            .pad(bottom = 2.vmin)
        )
        .add(93.ph, serverList
            .wrapScrolling()
        )
        .pad(5.vmin)
    )
}
