
package schwalbe.ventura.client

import kotlinx.serialization.Serializable
import schwalbe.ventura.engine.Language

@Serializable
enum class GameLanguage(override val id: String) : Language<GameLanguage> {
    GERMAN("de"),
    ENGLISH("en"),
    BULGARIAN("bg");
}

enum class LocalKeys {
    TEST,
    GREETING
}