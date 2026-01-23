
package schwalbe.ventura.client

import kotlinx.serialization.Serializable
import schwalbe.ventura.engine.Language
import schwalbe.ventura.engine.Localizations
import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.loadLanguages

@Serializable
enum class GameLanguage(override val id: String) : Language<GameLanguage> {
    GERMAN("de"),
    ENGLISH("en"),
    BULGARIAN("bg");
}

enum class LocalKeys {
    TITLE_SELECT_SERVER,
    BUTTON_ADD_SERVER,
    BUTTON_EDIT_SERVER,
    BUTTON_DELETE_SERVER,
    PLACEHOLDER_NO_SERVERS,

    TITLE_EDIT_SERVER,
    LABEL_SERVER_NAME,
    PLACEHOLDER_SERVER_NAME,
    LABEL_SERVER_ADDRESS,
    PLACEHOLDER_SERVER_ADDRESS,
    LABEL_SERVER_PORT,
    PLACEHOLDER_SERVER_PORT,
    BUTTON_SERVER_CONFIRM,
    BUTTON_SERVER_DISCARD,

    TITLE_CONNECTING_TO_SERVER,
    BUTTON_CANCEL_CONNECTION
}

val localized: Resource<Localizations<GameLanguage, LocalKeys>>
    = Localizations.loadLanguages("res/localizations", GameLanguage.ENGLISH)