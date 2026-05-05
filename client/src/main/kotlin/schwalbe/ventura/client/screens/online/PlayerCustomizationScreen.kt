
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.Client
import schwalbe.ventura.client.LocalKeys
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.game.PersonAnim
import schwalbe.ventura.client.localized
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.utils.parseRgbHex
import schwalbe.ventura.client.game.World
import schwalbe.ventura.net.Packet
import schwalbe.ventura.net.PacketType
import schwalbe.ventura.utils.toSerVector3
import org.joml.Vector3fc

private val COLOR_PALETTE: List<Vector3fc> = listOf(
    parseRgbHex("443331"),
    parseRgbHex("50473f"),
    parseRgbHex("705448"),
    parseRgbHex("6e7261"),
    parseRgbHex("97866f"),
    parseRgbHex("a8ac89"),
    parseRgbHex("d1c19e"),
    parseRgbHex("9ba9a0"),
    parseRgbHex("8a97a1"),
    parseRgbHex("816891"),
    parseRgbHex("aa749e"),
    parseRgbHex("cb8993"),
    parseRgbHex("d4a488"),
    parseRgbHex("d2ad72"),
    parseRgbHex("d3925b"),
    parseRgbHex("cc785b"),
    parseRgbHex("ba5e69"),
    parseRgbHex("94554d"),
    parseRgbHex("784a5d"),
    parseRgbHex("525979"),
    parseRgbHex("437f5d"),
    parseRgbHex("5a8b97"),
    parseRgbHex("86a063"),
    parseRgbHex("85b69a")
)

fun addColorSetting(
    colorSettings: Axis, name: LocalKeys,
    client: Client, onChange: () -> Unit
) {
    val i: Int = colorSettings.children.size
    val world: World = client.world ?: return
    val colors = Axis.row()
    val colorSize: UiSize = floor(100.pw / COLOR_PALETTE.size)
    for (color in COLOR_PALETTE) {
        val isSelected: Boolean = world.player.colors[i] == color.toSerVector3()
        colors.add(colorSize, Stack()
            .add(FlatBackground().withColor(color))
            .add(if (isSelected) {
                FlatBackground().withColor(Theme.FONT_COLOR)
                    .pad(25.ph)
            } else {
                Space()
            })
            .add(ClickArea().withLeftHandler {
                val colors = world.player.colors.toMutableList()
                colors[i] = color.toSerVector3()
                world.player.colors = colors
                client.network.outPackets?.send(Packet.serialize(
                    PacketType.CHANGE_PLAYER_COLORS, colors
                ))
                onChange()
            })
        )
    }
    colorSettings.add(2.vmin + 3.vmin + colorSize, Axis.column()
        .add(3.vmin, Text()
            .withText(localized()[name])
            .withSize(70.ph)
        )
        .add(colorSize, colors)
        .pad(horizontal = 0.px, vertical = 1.vmin)
    )
}

fun playerCustomizationScreen(client: Client): () -> GameScreen = {
    val colorSettings = Axis.column()
    var settingsDirty = false
    fun change() { settingsDirty = true }
    fun update() {
        colorSettings.disposeAll()
        addColorSetting(colorSettings, COLOR_OPTION_SKIN, client, ::change)
        addColorSetting(colorSettings, COLOR_OPTION_HAIR, client, ::change)
        addColorSetting(colorSettings, COLOR_OPTION_EYEBROWS, client, ::change)
        addColorSetting(colorSettings, COLOR_OPTION_HOODIE, client, ::change)
        addColorSetting(colorSettings, COLOR_OPTION_PANTS, client, ::change)
        addColorSetting(colorSettings, COLOR_OPTION_LEGS, client, ::change)
        addColorSetting(colorSettings, COLOR_OPTION_SHOES, client, ::change)
        addColorSetting(colorSettings, COLOR_OPTION_HANDS, client, ::change)
        addColorSetting(colorSettings, COLOR_OPTION_IRIS, client, ::change)
        addColorSetting(colorSettings, COLOR_OPTION_EYES, client, ::change)
    }
    update()
    val screen = PausedScreen(
        client,
        camMode = { w -> CameraModes.playerInRightHalf(w.player) },
        playerAnim = PersonAnim.thinking,
        render = {
            if (settingsDirty) {
                update()
                settingsDirty = false
            }
        }
    )
    screen.screen.add(layer = 0, element = Axis.row()
        .add(fpw * (1f/2f), Stack()
            .add(screen.background)
            .add(FlatBackground().withColor(Theme.PANEL_BACKGROUND))
            .add(Axis.column()
                .add(8.vmin, Text()
                    .withText(localized()[TITLE_CHARACTER_CUSTOMIZATION])
                    .withFont(googleSansSb())
                    .withSize(75.ph)
                    .pad(2.5.vmin)
                )
                .add(100.ph - 8.vmin, colorSettings
                    .pad(2.5.vmin)
                )
            )
        )
        .add(fpw * (1f/2f), Space())
    )
    screen.screen
}
