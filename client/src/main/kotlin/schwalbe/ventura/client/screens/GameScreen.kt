
package schwalbe.ventura.client.screens

import schwalbe.ventura.client.ApplicationScreen
import schwalbe.ventura.engine.ui.UiNavigator
import schwalbe.ventura.engine.ui.UiScreen
import schwalbe.ventura.net.PacketHandler

class GameScreen(
    override var render: () -> Unit = {},
    val networkState: () -> Unit = {},
    val packets: PacketHandler<Unit>? = null,
    navigator: UiNavigator<GameScreen>,
    onOpen: () -> Unit = {},
    onClose: () -> Unit = {}
) : UiScreen<GameScreen>(navigator, onOpen, onClose),
    ApplicationScreen<GameScreen>
