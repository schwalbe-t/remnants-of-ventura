
package schwalbe.ventura.server.game

import kotlinx.serialization.Serializable

@Serializable
data class Purse(
    var numCoins: Int = 0
)

fun Purse.tryRemoveCoins(numRemoved: Int): Boolean {
    if (numRemoved <= 0) { return false }
    if (this.numCoins < numRemoved) { return false }
    this.numCoins -= numRemoved
    return true
}

fun Purse.addCoins(numAdded: Int) {
    if (numAdded <= 0) { return }
    this.numCoins += numAdded
}
