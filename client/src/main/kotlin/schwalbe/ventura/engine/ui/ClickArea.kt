
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.input.*

class ClickArea : UiElement() {

    var isHovering: Boolean = false
        private set

    var leftDown: Boolean = false
    var onLeftDrag: ((UiElementContext) -> Unit)? = null
    var onLeftClick: ((UiElementContext) -> Unit)? = null

    var rightDown: Boolean = false
    var onRightDrag: ((UiElementContext) -> Unit)? = null
    var onRightClick: ((UiElementContext) -> Unit)? = null

    override val children: List<UiElement> = listOf()
    
    override fun captureInput(context: UiElementContext) {
        when {
            this.leftDown   -> this.onLeftDrag?.invoke(context)
            this.rightDown  -> this.onRightDrag?.invoke(context)
        }
        this.isHovering = Mouse.isInsideArea(
            context.visibleAbsLeft, context.visibleAbsTop,
            context.visibleAbsRight, context.visibleAbsBottom
        )
        for (e in context.global.nav.input.remainingOfType<MButtonEvent>()) {
            fun eatEvent() = context.global.nav.input.remove(e)
            when (e) {
                is MButtonDown -> {
                    if (!this.isHovering) { continue }
                    when (e.button) {
                        MButton.LEFT    -> this.leftDown = true
                        MButton.RIGHT   -> this.rightDown = true
                        else -> {}
                    }
                }
                is MButtonUp -> {
                    when (e.button) {
                        MButton.LEFT    -> this.leftDown = false
                        MButton.RIGHT   -> this.rightDown = false
                        else -> {}
                    }
                    if (!this.isHovering) { continue }
                    when (e.button) {
                        MButton.LEFT -> this.onLeftClick?.let {
                            it(context)
                            eatEvent()
                        }
                        MButton.RIGHT -> this.onRightClick?.let {
                            it(context)
                            eatEvent()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun withCtxLeftHandler(f: ((UiElementContext) -> Unit)?): ClickArea {
        this.onLeftClick = f
        return this
    }

    fun withCtxRightHandler(f: ((UiElementContext) -> Unit)?): ClickArea {
        this.onRightClick = f
        return this
    }

    fun withLeftHandler(f: (() -> Unit)?)
        = this.withCtxLeftHandler(f?.let { f -> { f() } })

    fun withRightHandler(f: (() -> Unit)?): ClickArea
        = this.withCtxRightHandler(f?.let { f -> { f() } })

    fun withCtxLeftDragHandler(f: ((UiElementContext) -> Unit)?): ClickArea {
        this.onLeftDrag = f
        return this
    }

    fun withCtxRightDragHandler(f: ((UiElementContext) -> Unit)?): ClickArea {
        this.onRightDrag = f
        return this
    }
    
}