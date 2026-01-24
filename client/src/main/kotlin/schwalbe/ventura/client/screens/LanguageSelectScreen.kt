
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*

private fun createLanguageOption(
    language: GameLanguage, client: Client
): UiElement = createButton(
    content = Axis.column()
        .add(60.ph, Text()
            .withFont(googleSansSb())
            .withText(language.nativeName)
            .withSize(70.ph)
        )
        .add(40.ph, Text()
            .withText(language.englishName)
            .withSize(70.ph)
            .withColor(SECONDARY_FONT_COLOR)
        )
        .pad(1.5.vmin),
    handler = {
        client.config.language = language
        client.config.write()
        localized().changeLanguage(language)
        client.nav.clear(mainScreen(client))
    }
).pad(bottom = 1.5.vmin)

fun languageSelectScreen(client: Client): GameScreen {
    val screen = GameScreen(
        render = renderGridBackground(client),
        networkState = noNetworkConnections(client),
        navigator = client.nav
    )
    val languageList = Axis.column()
    for (language in GameLanguage.entries) {
        languageList.add(9.5.vmin, createLanguageOption(language, client))
    }
    screen.add(layer = 0, element = Axis.column()
        .add(7.ph, Text()
            .withFont(googleSansSb())
            .withText(localized()[TITLE_SELECT_LANGUAGE])
            .withSize(2.5.vmin)
        )
        .add(93.ph - 5.vmin, languageList
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
    return screen
}
