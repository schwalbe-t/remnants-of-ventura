
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.ui.UiNavigator
import schwalbe.ventura.engine.ui.UiScreen
import schwalbe.ventura.net.PacketHandler

class GameScreen(
    val render: () -> Unit = {},
    val networkState: () -> Unit = {},
    val packets: PacketHandler<Unit>? = null,
    navigator: UiNavigator<GameScreen>
) : UiScreen<GameScreen>(navigator)
