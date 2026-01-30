
package schwalbe.ventura.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Paths
import java.util.Locale

interface Language<L : Language<L>> {
    /**
     * The ISO 639-1 language code, e.g. 'en', 'bg', 'de', 'fr', ...
     */
    val id: String
}

/**
 * Attempts to find a language in the given language enum that has an ID
 * matching that of the current system locale. This requires that the ID
 * is a ISO 639-1 language code (e.g. 'en', 'fr').
 * @param default The language to return if the current system language matches
 * none of the languages present in the enum
 * @return The found language or the default
 */
inline fun <reified L> findSystemLanguage(default: L): L
    where L : Enum<L>, L : Language<L> {
    val locale = Locale.getDefault()
    val sysId: String = locale.language
    return enumValues<L>()
        .firstOrNull { it.id == sysId }
        ?: default
}

class Localizations<L : Language<L>, K : Enum<K>>(
    val languages: Map<String, LoadedLanguage>,
    var current: LoadedLanguage
) {

    companion object;

    @Serializable
    data class LoadedLanguage(
        val strings: Map<String, String>
    )

    fun changeLanguage(language: L) {
        this.current = this.languages[language.id]!!
    }

    operator fun get(key: K): String = this[key.name]

    operator fun get(rawKey: String): String
        = this.current.strings[rawKey] ?: ""

}

fun <K, L> loadLanguageFile(
    language: L, file: String, path: String, keys: Set<K>
): Localizations.LoadedLanguage
    where K : Enum<K>, L : Enum<L>, L : Language<L> {
    val raw: String = File(path).readText()
    val loaded = Json.decodeFromString<Localizations.LoadedLanguage>(raw)
    keys.filter { it.name !in loaded.strings.keys }.forEach {
        println(
            "Localization for language '${language.name}' ('$file') " +
            "is missing key '${it.name}'"
        )
    }
    return loaded
}

inline fun <reified K, reified L> Localizations.Companion.loadLanguages(
    directory: String, default: L
): Resource<Localizations<L, K>>
    where K : Enum<K>, L : Language<L>, L : Enum<L>
= Resource {
    val keys: Set<K> = enumValues<K>().toSet()
    val languages: Array<L> = enumValues<L>()
    val loaded: Map<String, Localizations.LoadedLanguage>
        = languages.associateBy({ it.id }, {
            val file = "${it.id}.json"
            val path: String = Paths.get(directory, file).toString()
            loadLanguageFile(it, file, path, keys)
        })
    val current: Localizations.LoadedLanguage = loaded[default.id]!!
    { Localizations(loaded, current) }
}