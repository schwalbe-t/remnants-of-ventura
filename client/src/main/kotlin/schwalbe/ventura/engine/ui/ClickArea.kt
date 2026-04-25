
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.input.*

class ClickArea : UiElement() {

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
        val mouseOver: Boolean = Mouse.isInsideArea(
            context.visibleAbsLeft, context.visibleAbsTop,
            context.visibleAbsRight, context.visibleAbsBottom
        )
        for (e in context.global.nav.input.remainingOfType<MButtonEvent>()) {
            when (e) {
                is MButtonDown -> {
                    if (!mouseOver) { continue }
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
                    if (!mouseOver) { continue }
                    when (e.button) {
                        MButton.LEFT    -> this.onLeftClick?.invoke(context)
                        MButton.RIGHT   -> this.onRightClick?.invoke(context)
                        else -> {}
                    }
                }
            }
            context.global.nav.input.remove(e)
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