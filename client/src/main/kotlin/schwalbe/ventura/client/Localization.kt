
package schwalbe.ventura.client

import schwalbe.ventura.engine.Language
import schwalbe.ventura.engine.Localizations
import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.loadLanguages
import kotlinx.serialization.Serializable

@Serializable
enum class GameLanguage(
    override val id: String, val nativeName: String, val englishName: String
) : Language<GameLanguage> {
    ENGLISH     ("en", "English",   "English"   ),
    GERMAN      ("de", "Deutsch",   "German"    ),
    BULGARIAN   ("bg", "Български", "Bulgarian" );
}

enum class LocalKeys {
    // Common Button Strings
    BUTTON_GO_BACK,
    BUTTON_DISCONNECT,

    // Main Menu Screen
    BUTTON_PLAY,
    BUTTON_CHANGE_LANGUAGE,
    BUTTON_EXIT,

    // Language Select Screen
    TITLE_SELECT_LANGUAGE,

    // Server List Screen
    TITLE_SELECT_SERVER,
    BUTTON_ADD_SERVER,
    BUTTON_EDIT_SERVER,
    BUTTON_DELETE_SERVER,
    PLACEHOLDER_NO_SERVERS,
    LABEL_LOGGED_IN_AS,

    // Server Edit Screen
    TITLE_EDIT_SERVER,
    LABEL_SERVER_NAME,
    PLACEHOLDER_SERVER_NAME,
    LABEL_SERVER_ADDRESS,
    PLACEHOLDER_SERVER_ADDRESS,
    LABEL_SERVER_PORT,
    PLACEHOLDER_SERVER_PORT,
    BUTTON_SERVER_CONFIRM,
    BUTTON_SERVER_DISCARD,

    // Connecting To Server Screen
    TITLE_CONNECTING_TO_SERVER,
    BUTTON_CANCEL_CONNECTION,

    // Connection Failed Screen
    TITLE_CONNECTION_TO_SERVER_FAILED,

    // Login Screen
    TITLE_LOGIN,
    TITLE_SIGN_UP,
    LABEL_USERNAME,
    PLACEHOLDER_USERNAME,
    LABEL_PASSWORD,
    PLACEHOLDER_PASSWORD,
    LABEL_REPEAT_PASSWORD,
    PLACEHOLDER_REPEAT_PASSWORD,
    BUTTON_LOG_IN,
    BUTTON_SIGN_UP,

    // Inventory Screen
    TITLE_INVENTORY,
    PLACEHOLDER_INVENTORY_EMPTY,

    // Robot Editing Screen
    LABEL_ROBOT_STAT_HEALTH,
    LABEL_ROBOT_STAT_MEMORY,
    LABEL_ROBOT_STAT_PROCESSOR,
    BUTTON_ROBOT_START,
    BUTTON_ROBOT_STOP,
    TITLE_SELECT_SOURCE_FILE,
    PLACEHOLDER_NO_SOURCE_FILES,
    TITLE_ROBOT_ATTACHMENTS,
    TITLE_ROBOT_CODE_FILES,
    BUTTON_ADD_CODE_FILE,
    BUTTON_DESTROY_ROBOT,

    // Escape Menu Screen
    BUTTON_BACK_TO_GAME,
    BUTTON_LOG_OUT,

    // Error Strings
    ERROR_INVALID_ACCOUNT_PARAMS,
    ERROR_INVALID_ACCOUNT_CREDS,
    ERROR_SESSION_CREATION_COOLDOWN,
    ERROR_INVALID_SESSION_CREDS,
    ERROR_ACCOUNT_ALREADY_ONLINE,
    ERROR_PASSWORDS_DONT_MATCH,
    ERROR_USERNAME_TOO_SHORT,
    ERROR_PASSWORD_TOO_SHORT,
}

val localized: Resource<Localizations<GameLanguage, LocalKeys>>
    = Localizations.loadLanguages("res/localizations", GameLanguage.ENGLISH)