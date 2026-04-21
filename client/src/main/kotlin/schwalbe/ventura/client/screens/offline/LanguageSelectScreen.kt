
package schwalbe.ventura.client.screens.offline

import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.screens.*

private fun createLanguageOption(
    language: GameLanguage, client: Client
): UiElement = Theme.button(
    content = Axis.column()
        .add(60.ph, Text()
            .withFont(googleSansSb())
            .withText(language.nativeName)
            .withSize(70.ph)
        )
        .add(40.ph, Text()
            .withText(language.englishName)
            .withSize(70.ph)
            .withColor(Theme.SECONDARY_FONT_COLOR)
        )
        .pad(1.5.vmin),
    handler = {
        client.config.language = language
        client.config.write()
        localized().changeLanguage(language)
        client.nav.clear(serverSelectScreen(client))
    }
).pad(bottom = 1.5.vmin)

fun languageSelectScreen(client: Client): () -> GameScreen = {
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
    val languageList = Axis.column()
    for (language in GameLanguage.entries) {
        languageList.add(9.5.vmin, createLanguageOption(language, client))
    }
    languageList.add(50.ph, Space())
    root.add(Axis.column()
        .add(100.ph - 5.vmin, languageList
            .wrapScrolling()
        )
        .add(5.vmin, Theme.button(
            content = localized()[BUTTON_GO_BACK],
            handler = {
                client.nav.pop()
            }
        ).pad(top = 1.vmin, right = 100.pw - 30.vmin))
        .pad(5.vmin)
        .withTitlebar(localized()[TITLE_SELECT_LANGUAGE])
    )
    screen.add(layer = 0, element = root)
    screen
}
