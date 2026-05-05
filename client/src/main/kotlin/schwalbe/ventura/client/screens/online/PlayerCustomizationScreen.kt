
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.Client
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.game.PersonAnim
import schwalbe.ventura.client.localized
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.game.World
import schwalbe.ventura.net.Packet
import schwalbe.ventura.net.PacketType
import schwalbe.ventura.data.PersonStyle
import schwalbe.ventura.data.PersonHairStyle
import schwalbe.ventura.PaletteColor
import schwalbe.ventura.data.PersonColorType

fun addHairSettings(
    settings: Axis, client: Client, onChange: () -> Unit
) {
    val world: World = client.world ?: return
    val hairOptions = Axis.row()
    for (hairstyle in PersonHairStyle.entries) {
        val isSelected: Boolean = world.player.style.hair == hairstyle
        hairOptions.add(10.vmin, Stack()
            .add(if (!isSelected) {
                FlatBackground()
                    .withColor(Theme.BUTTON_COLOR)
                    .withHoverColor(Theme.BUTTON_HOVER_COLOR)
            } else {
                FlatBackground()
                    .withColor(Theme.BUTTON_HOVER_COLOR)
            })
            .add(Text()
                .withText(localized()[hairstyle.localNameKey])
                .withFont(googleSansSb())
                .withSize(80.ph)
                .alignCenter()
                .pad(0.75.vmin)
            )
            .add(ClickArea().withLeftHandler {
                if (isSelected) { return@withLeftHandler }
                world.player.style = PersonStyle(
                    colors = world.player.style.colors,
                    hair = hairstyle
                )
                onChange()
            })
            .wrapBorderRadius(0.75.vmin)
            .pad(right = 1.vmin)
        )
    }
    settings.add(2.vmin + 3.vmin + 3.5.vmin, Axis.column()
        .add(3.vmin, Text()
            .withText(localized()[SECTION_HAIR_OPTIONS])
            .withSize(70.ph)
        )
        .add(3.5.vmin, hairOptions)
        .pad(horizontal = 0.px, vertical = 1.vmin)
    )
}

fun addColorSetting(
    settings: Axis, i: Int, name: String,
    client: Client, onChange: () -> Unit
) {
    val world: World = client.world ?: return
    val colors = Axis.row()
    val colorSize: UiSize = floor(100.pw / PaletteColor.entries.size)
    for (color in PaletteColor.entries) {
        val isSelected: Boolean
            = world.player.style.colors[i] == color.ser
        colors.add(colorSize, Stack()
            .add(FlatBackground().withColor(color.norm))
            .add(if (isSelected) {
                FlatBackground().withColor(Theme.FONT_COLOR)
                    .pad(25.ph)
            } else {
                Space()
            })
            .add(ClickArea().withLeftHandler {
                if (isSelected) { return@withLeftHandler }
                val colors = world.player.style.colors.toMutableList()
                colors[i] = color.ser
                world.player.style = PersonStyle(
                    colors = colors,
                    hair = world.player.style.hair
                )
                onChange()
            })
        )
    }
    settings.add(2.vmin + 3.vmin + colorSize, Axis.column()
        .add(3.vmin, Text()
            .withText(name)
            .withSize(70.ph)
        )
        .add(colorSize, colors)
        .pad(horizontal = 0.px, vertical = 1.vmin)
    )
}

fun playerCustomizationScreen(client: Client): () -> GameScreen = {
    val settings = Axis.column()
    var settingsDirty = false
    fun onChange() {
        val world: World = client.world ?: return
        client.network.outPackets?.send(Packet.serialize(
            PacketType.CHANGE_PLAYER_STYLE,
            world.player.style
        ))
        settingsDirty = true
    }
    fun update() {
        settings.disposeAll()
        addHairSettings(settings, client, ::onChange)
        for (colorType in PersonColorType.entries) {
            addColorSetting(
                settings, colorType.ordinal,
                localized()[colorType.localNameKey], client, ::onChange
            )
        }
        settings.add(50.ph, Space())
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
                .add(100.ph - 8.vmin, settings
                    .wrapThemedScrolling(vert = true, horiz = false)
                    .pad(2.5.vmin)
                )
            )
        )
        .add(fpw * (1f/2f), Space())
    )
    screen.screen
}
