
package schwalbe.ventura.server.game.extensions

import kotlinx.serialization.Serializable
import schwalbe.ventura.server.game.*

data class GameAttachmentContext(
    val world: World,
    val player: Player,
    val robot: Robot
)

@Serializable
sealed interface GameAttachment {
    fun update(ctx: GameAttachmentContext) {}
}