
package schwalbe.ventura.data

import kotlinx.serialization.Serializable

@Serializable
data class Trade(
    val item: Item,
    val count: Int = 1,
    val price: Int
)
