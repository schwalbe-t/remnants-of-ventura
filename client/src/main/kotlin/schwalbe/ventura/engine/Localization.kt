
package schwalbe.ventura.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.File
import java.nio.file.Paths

interface Language<L : Language<L>> {
    val id: String
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

    operator fun get(key: K): String
        = this.current.strings[key.name] ?: ""

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