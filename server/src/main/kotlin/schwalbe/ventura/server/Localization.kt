package schwalbe.ventura.server

import kotlinx.serialization.json.Json
import schwalbe.ventura.data.RemoteLocalization
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

class Localizations(
    val languages: Map<String, RemoteLocalization>,
    val fallbackLocale: String
) {

    companion object;

    private fun parseSelector(raw: String): (String) -> Boolean {
        when {
            raw.startsWith("*") -> {
                val suffix: String = raw.substring(1)
                return { it.endsWith(suffix) }
            }
            raw.endsWith("*") -> {
                val prefix: String = raw.substring(0, raw.length - 1)
                return { it.startsWith(prefix) }
            }
            else -> return { it == raw }
        }
    }

    private fun tryGetDialogue(
        locale: String, selector: (String) -> Boolean
    ): List<RemoteLocalization.Dialogue>? {
        val language: RemoteLocalization = this.languages[locale] ?: return null
        val options: List<String> = language.dialogue.keys.filter(selector)
        val key: String = options.randomOrNull() ?: return null
        return language.dialogue[key]
    }

    fun getDialogue(
        locale: String, rawSelector: String
    ): List<RemoteLocalization.Dialogue> {
        val selector: (String) -> Boolean = this.parseSelector(rawSelector)
        return this.tryGetDialogue(locale, selector)
            ?: this.tryGetDialogue(this.fallbackLocale, selector)
            ?: listOf()
    }

}

fun Localizations.Companion.readDirectory(
    dirPath: Path, fallbackLocale: String
): Localizations {
    val loaded = mutableMapOf<String, RemoteLocalization>()
    for (localeFile in dirPath.listDirectoryEntries()) {
        if (localeFile.extension != "json") { continue }
        try {
            val localeId: String = localeFile.nameWithoutExtension
            val rawData: String = Files.readString(localeFile)
            val data: RemoteLocalization = Json.decodeFromString(rawData)
            loaded[localeId] = data
        } catch (e: Exception) {
            System.err.println("Failed to parse localization '$localeFile': $e")
        }
    }
    println("Loaded localizations for ${loaded.size} language(s)")
    return Localizations(loaded, fallbackLocale)
}
