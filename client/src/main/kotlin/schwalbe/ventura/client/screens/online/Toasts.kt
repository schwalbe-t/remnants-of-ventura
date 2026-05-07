
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.client.Client
import schwalbe.ventura.client.LocalKeys
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.SoundEffects
import schwalbe.ventura.client.localized
import schwalbe.ventura.client.screens.Theme
import schwalbe.ventura.client.screens.googleSansSb
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.net.PacketHandler
import schwalbe.ventura.net.PacketType
import schwalbe.ventura.net.TaggedErrorPacket

class ToastManager {
    var current: String? = null
    var displayUntil: Long = Long.MAX_VALUE

    fun update() {
        if (this.displayUntil <= System.currentTimeMillis()) {
            this.clear()
        }
    }

    fun show(text: LocalKeys, durationMs: Long = 3000) {
        this.current = localized()[text]
        this.displayUntil = System.currentTimeMillis() + durationMs
    }

    fun clear() {
        this.current = null
        this.displayUntil = Long.MAX_VALUE
    }
}

class ToastDisplay(
    val manager: ToastManager,
    val width: UiSize = 50.vw,
    val height: UiSize = 4.vmin
) {

    val background = BlurBackground().withRadius(2).withSpread(4)
    val text = Text()
        .withFont(googleSansSb())
        .withSize(80.ph)
        .alignCenter()
    val container = Stack()
        .add(this.background)
        .add(FlatBackground().withColor(Theme.TOAST_BACKGROUND))
        .add(this.text.pad(0.75.vmin))
        .wrapBorderRadius(0.75.vmin)
    val root = this.container.pad(
        horizontal = (100.vw - this.width) / 2,
        vertical = 5.vmin
    )

    var current: String? = null

    private fun updateState(new: String?) {
        if (new == null) {
            this.container.withSize(0.px, 0.px)
        } else {
            this.text.withText(new)
            this.container.withSize(this.width, this.height)
        }
        this.current = new
    }

    fun update() {
        this.manager.update()
        val old: String? = this.current
        val new: String? = this.manager.current
        if (old != new) {
            this.updateState(new)
        }
        this.background.invalidate()
    }

    init {
        this.updateState(null)
    }

}

val TAGGED_ERROR_TOASTS: Map<TaggedErrorPacket, LocalKeys> = mapOf(
    TaggedErrorPacket.NO_SPACE_FOR_ROBOT
        to ERROR_NO_SPACE_FOR_ROBOT,
    TaggedErrorPacket.ROBOT_NOT_IN_MAINTENANCE_AREA
        to ERROR_NOT_IN_MAINTENANCE_AREA,
    TaggedErrorPacket.ROBOT_STOPPED_DURING_CHALLENGE
        to ERROR_STOPPED_DURING_CHALLENGE
)

fun PacketHandler<Unit>.displayTaggedErrorToasts(
    display: ToastDisplay, client: Client,
    mapping: Map<TaggedErrorPacket, LocalKeys> = TAGGED_ERROR_TOASTS
): PacketHandler<Unit> = this.onPacket(PacketType.TAGGED_ERROR) { err, _ ->
    display.manager.show(mapping[err] ?: return@onPacket)
    client.sounds.play(SoundEffects.ERROR())
}
