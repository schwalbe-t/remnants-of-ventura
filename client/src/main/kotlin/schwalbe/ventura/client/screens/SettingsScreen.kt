
package schwalbe.ventura.client.screens

import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.engine.input.Mouse
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.net.PacketHandler
import kotlin.math.roundToInt

private fun UiElement.padSection(): UiElement
    = this.pad(horizontal = 1.vmin, vertical = 0.5.vmin)

private sealed interface SettingValue {
    class Toggle(
        val init: Boolean,
        val confirm: (value: Boolean, confirm: (Boolean) -> Unit) -> Unit
            = { value, confirm -> confirm(value) },
        val onChange: (Boolean) -> Unit
    ) : SettingValue
    class Number(
        val min: Float, val max: Float, val step: Float?,
        val init: Float,
        val onChange: (Float) -> Unit,
        val display: (Float) -> String = Float::toString
    ): SettingValue
}

private class Setting(
    val name: LocalKeys,
    val description: LocalKeys?,
    val value: SettingValue
)

private fun createSettingValueElem(setting: SettingValue) = when (setting) {
    is SettingValue.Toggle -> {
        var value: Boolean = setting.init
        val display = Stack()
        fun updateDisplay() {
            display.disposeAll()
            display.add(when (value) {
                true -> Icons.CHECK_BOX_CHECKED.create(100.ph)
                false -> Icons.CHECK_BOX_BLANK.create(100.ph)
            })
        }
        updateDisplay()
        val lineThickness: UiSize = 2.px
        Axis.row()
            .add(100.pw - 100.ph - 1.vmin, FlatBackground()
                .withColor(Theme.BUTTON_HOVER_COLOR)
                .pad(
                    horizontal = 0.px,
                    vertical = (100.ph - lineThickness) / 2
                )
            )
            .add(1.vmin, Space())
            .add(100.ph, Stack()
                .add(display)
                .add(ClickArea().withLeftHandler {
                    setting.confirm(!value) { confirmed ->
                        value = confirmed
                        updateDisplay()
                        setting.onChange(confirmed)
                    }
                })
            )
    }
    is SettingValue.Number -> {
        val valRange: Float = setting.max - setting.min
        val filler = FlatBackground()
            .withColor(Theme.FONT_COLOR)
            .wrapBorderRadius(50.ph)
        val knob = FlatBackground()
            .withColor(Theme.SECONDARY_FONT_COLOR)
            .wrapBorderRadius(50.ph)
            .pad(0.5.vmin)
            .pad()
        val display = Text()
            .withFont(jetbrainsMonoSb())
            .withSize(75.ph)
        fun update(value: Float) {
            val norm = (value - setting.min) / valRange
            val left = ceil((100.pw - 100.ph) * norm)
            knob.paddingLeft = left
            knob.paddingRight = floor(100.pw - 100.ph - left)
            filler.width = left + 100.ph
            display.withText(setting.display(value))
            if (norm >= 0.5) {
                display.alignLeft()
                display.withColor(130, 130, 130)
            } else {
                display.alignRight()
                display.withColor(Theme.SECONDARY_FONT_COLOR)
            }
        }
        update(setting.init)
        Stack()
            .add(FlatBackground().withColor(Theme.FONT_COLOR))
            .add(Stack()
                .add(BlurBackground().withRadius(5))
                .add(FlatBackground().withColor(Theme.PANEL_BACKGROUND))
                .wrapBorderRadius(50.ph)
                .pad(ceil(0.2.vmin))
            )
            .add(filler)
            .add(knob)
            .add(display.pad(vertical = 0.25.vmin, horizontal = 1.vmin))
            .add(ClickArea().withCtxLeftDragHandler { ctx ->
                val leftEnd: Float = ctx.absPxX + (50.ph)(ctx)
                val relX: Float = Mouse.position.x() - leftEnd
                val totalWidth: Float = (100.pw - 100.ph)(ctx)
                val raw: Float = setting.min + (relX / totalWidth * valRange)
                var value: Float = minOf(maxOf(raw, setting.min), setting.max)
                setting.step?.let { step ->
                    value = (value / step).roundToInt() * step
                }
                update(value)
                setting.onChange(value)
            })
            .wrapBorderRadius(50.ph)
            .pad(horizontal = 0.px, vertical = 0.35.vmin)
    }
}

private fun addSection(
    sections: Axis,
    title: LocalKeys,
    description: LocalKeys?,
    settings: Iterable<Setting>
) {
    val l = localized()
    val content = Axis.column()
    content.add(1.vmin, Space())
    content.add(3.vmin, Text()
        .withText(l[title])
        .withFont(googleSansSb())
        .withSize(75.ph)
        .pad(horizontal = 1.vmin, vertical = 0.px)
    )
    if (description != null) {
        content.add(3.vmin, Text()
            .withText(l[description])
            .withSize(1.25.vmin)
            .wrapThemedScrolling(horiz = false, vert = true)
            .pad(horizontal = 1.vmin, vertical = 0.px)
        )
    }
    content.add(0.5.vmin, Space())
    for (setting in settings) {
        val description: String = setting.description?.let { l[it] } ?: ""
        content.add(5.5.vmin, Axis.column()
            .add(100.ph - 1.5.vmin, Axis.row()
                .add(38.2.pw, Text()
                    .withText(l[setting.name])
                    .withFont(googleSansSb())
                    .withSize(80.ph)
                    .pad(horizontal = 0.px, vertical = 0.5.vmin)
                )
                .add(61.8.pw, createSettingValueElem(setting.value))
            )
            .add(1.75.vmin, Text()
                .withText(description)
                .withSize(1.25.vmin)
                .wrapThemedScrolling(horiz = false, vert = true)
            )
            .pad(horizontal = 1.vmin, vertical = 0.5.vmin)
        )
    }
    content.add(0.5.vmin, Space())
    val sectionSize: UiSize = content.computeContentLength() + 2 * 0.5.vmin
    sections.add(sectionSize, Stack()
        .add(BlurBackground().withRadius(5))
        .add(FlatBackground().withColor(Theme.PANEL_BACKGROUND))
        .add(content)
        .wrapBorderRadius(1.vmin)
        .padSection()
    )
}

private fun changeSettings(client: Client, f: Settings.() -> Unit) {
    f(client.config.settings)
    client.config.write()
}

fun settingsScreen(
    client: Client,
    networkState: () -> Unit,
    packets: PacketHandler<Unit>? = null,
    renderBackground: (root: UiElement) -> Unit,
    onClose: () -> Unit = {}
): () -> GameScreen = {
    val root = Stack()
    val screen = GameScreen(
        render = { renderBackground(root) },
        networkState = networkState,
        packets = packets,
        navigator = client.nav,
        onClose = onClose
    )
    val sections = Axis.column()
    sections.add(0.5.vmin, Space())
    addSection(
        sections,
        title = SECTION_AUDIO_SETTINGS,
        description = DESCRIPTION_AUDIO_SETTINGS,
        settings = listOf(
            Setting(
                SETTING_GENERAL_VOLUME, DESCRIPTION_GENERAL_VOLUME,
                SettingValue.Number(
                    min = 0f, max = 2f, step = 0.01f,
                    init = client.config.settings.generalVolume,
                    onChange = { changeSettings(client) {
                        this.generalVolume = it
                        this.applyAudio(client)
                    } },
                    display = { "${(it * 100f).roundToInt()}%" }
                )
            ),
            Setting(
                SETTING_MUSIC_VOLUME, DESCRIPTION_MUSIC_VOLUME,
                SettingValue.Number(
                    min = 0f, max = 1f, step = 0.01f,
                    init = client.config.settings.musicVolume,
                    onChange = { changeSettings(client) {
                        this.musicVolume = it
                        this.applyAudio(client)
                    } },
                    display = { "${(it * 100f).roundToInt()}%" }
                )
            ),
            Setting(
                SETTING_SFX_VOLUME, DESCRIPTION_SFX_VOLUME,
                SettingValue.Number(
                    min = 0f, max = 1f, step = 0.01f,
                    init = client.config.settings.sfxVolume,
                    onChange = { changeSettings(client) {
                        this.sfxVolume = it
                        this.applyAudio(client)
                    } },
                    display = { "${(it * 100f).roundToInt()}%" }
                )
            )
        )
    )
    addSection(
        sections,
        title = SECTION_DISPLAY_SETTINGS,
        description = DESCRIPTION_DISPLAY_SETTINGS,
        settings = listOf(
            Setting(
                SETTING_FULLSCREEN, DESCRIPTION_FULLSCREEN,
                SettingValue.Toggle(
                    init = client.config.settings.fullscreenEnabled,
                    onChange = { changeSettings(client) {
                        this.fullscreenEnabled = it
                        this.applyDisplay(client)
                    } }
                )
            ),
            Setting(
                SETTING_VSYNC, DESCRIPTION_VSYNC,
                SettingValue.Toggle(
                    init = client.config.settings.vsyncEnabled,
                    onChange = { changeSettings(client) {
                        this.vsyncEnabled = it
                        this.applyDisplay(client)
                    } }
                )
            )
        )
    )
    addSection(
        sections,
        title = SECTION_CODE_EDITING_SETTINGS,
        description = DESCRIPTION_CODE_EDITING_SETTINGS,
        settings = listOf(
            Setting(
                SETTING_ADVANCED_EDITING, DESCRIPTION_ADVANCED_EDITING,
                SettingValue.Toggle(
                    init = client.config.settings.advancedEditingEnabled,
                    confirm = { isEnabled, confirm ->
                        if (!isEnabled) {
                            confirm(false)
                        } else {
                            showAdvancedEditingConfirmation(
                                client, onSelect = confirm
                            )
                        }
                    },
                    onChange = { changeSettings(client) {
                        this.advancedEditingEnabled = it
                    } }
                )
            ),
            Setting(
                SETTING_AUTO_RESTART, DESCRIPTION_AUTO_RESTART,
                SettingValue.Toggle(
                    init = client.config.settings.autoRestartEnabled,
                    onChange = { changeSettings(client) {
                        this.autoRestartEnabled = it
                    } }
                )
            )
        )
    )
    sections.add(50.ph, Space())
    root.add(Axis.row()
        .add(50.pw, Axis.column()
            .add(100.ph - 6.vmin, sections
                .wrapThemedScrolling(horiz = false, vert = true)
            )
            .add(6.vmin, Theme.button(localized()[BUTTON_GO_BACK], handler = {
                client.nav.pop()
            }).pad(right = 50.pw).pad(1.vmin))
        )
        .add(50.pw, Space())
        .withTitlebar(localized()[TITLE_SETTINGS])
    )
    screen.add(layer = 0, element = root)
    screen
}
