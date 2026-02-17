
package schwalbe.ventura.server.game

import kotlinx.serialization.Serializable
import schwalbe.ventura.data.Item
import kotlin.collections.MutableMap

@Serializable
data class Inventory(
    val itemCounts: MutableMap<Item, Int> = mutableMapOf()
)

operator fun Inventory.get(item: Item): Int
    = this.itemCounts[item] ?: 0

fun Inventory.tryRemove(item: Item, count: Int = 1): Boolean {
    if (count <= 0) { return false }
    val presentCount: Int = this[item]
    if (presentCount < count) { return false }
    this.itemCounts[item] = presentCount - count
    return true
}

fun Inventory.add(item: Item, count: Int = 1) {
    if (count <= 0) { return }
    val presentCount: Int = this[item]
    this.itemCounts[item] = presentCount + count
}
