
package schwalbe.ventura.server.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import schwalbe.ventura.data.ItemType
import schwalbe.ventura.data.Trade
import java.nio.file.Files
import java.nio.file.Path

@Serializable
class Trades(
    val trades: Map<String, List<Trade>> = mapOf(),
    val pawns: Map<ItemType, Int> = mapOf()
) {

    companion object;

}

fun Trades.Companion.readFile(path: Path): Trades {
    try {
        val raw: String = Files.readString(path)
        val parsed: Trades = Json.decodeFromString(raw)
        return parsed
    } catch (e: Exception) {
        System.err.println("Failed to parse trades '$path': $e")
        return Trades()
    }
}
