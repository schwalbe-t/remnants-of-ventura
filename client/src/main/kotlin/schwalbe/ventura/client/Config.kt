package schwalbe.ventura.client

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import schwalbe.ventura.engine.findSystemLanguage
import java.io.File
import kotlin.uuid.Uuid

@Serializable
data class Config(
    var language: GameLanguage,
    val servers: MutableList<Server> = mutableListOf(),
    val sessions: MutableMap<String, Uuid> = mutableMapOf()
) {
    @Serializable
    data class Server(
        val name: String,
        val address: String,
        val port: Int
    )

    companion object {
        const val PATH: String = "config.json"
    }
}

fun Config.Companion.makeDefault(): Config {
    val language = findSystemLanguage(GameLanguage.ENGLISH)
    return Config(language)
}

fun Config.Companion.read(): Config {
    try {
        val src: String = File(Config.PATH).readText()
        return Json.decodeFromString(src)
    } catch (e: Exception) {
        return Config.makeDefault().write()
    }
}

fun Config.write(): Config {
    val json = Json {
        prettyPrint = true
        prettyPrintIndent = " ".repeat(4)
    }
    val src: String = json.encodeToString(this)
    File(Config.PATH).writeText(src)
    return this
}