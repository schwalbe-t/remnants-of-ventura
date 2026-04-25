package schwalbe.ventura.client

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import schwalbe.ventura.engine.findSystemLanguage
import java.io.File
import kotlin.uuid.Uuid

@Serializable
data class Config(
    var language: GameLanguage = findSystemLanguage(GameLanguage.ENGLISH),
    val settings: Settings = Settings(),
    val servers: MutableList<Server> = mutableListOf(),
    val sessions: MutableMap<String, Session> = mutableMapOf()
) {
    @Serializable
    data class Server(
        val name: String,
        val address: String,
        val port: Int
    )

    @Serializable
    data class Session(
        val username: String,
        val token: Uuid
    )

    companion object {
        const val PATH: String = "config.json"
    }
}

fun Config.Companion.read(): Config {
    try {
        val src: String = File(Config.PATH).readText()
        return Json.decodeFromString(src)
    } catch (e: Exception) {
        return Config().write()
    }
}

fun Config.write(): Config {
    val json = Json {
        prettyPrint = true
        prettyPrintIndent = " ".repeat(4)
        encodeDefaults = true
    }
    val src: String = json.encodeToString(this)
    File(Config.PATH).writeText(src)
    return this
}