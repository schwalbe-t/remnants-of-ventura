
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.DepthTesting
import schwalbe.ventura.engine.gfx.Shader
import schwalbe.ventura.engine.gfx.Texture
import org.joml.*

open class UiScreen<S : UiScreen<S>>(
    val nav: UiNavigator<S>,
    val onOpen: () -> Unit
) {
    
    private data class BaseElement(val element: UiElement, val layer: Int)

    var currentlyInFocus: Focusable? = null
        set(value) {
            if (field !== value) {
                field?.onFocusLost()
            }
            field = value
        }
    
    private val elements: MutableList<BaseElement> = mutableListOf()
    
    fun add(element: UiElement, layer: Int = 0) {
        this.elements.add(BaseElement(element, layer))
        this.elements.sortBy(BaseElement::layer)
    }
    
    fun remove(element: UiElement) {
        this.elements.removeIf { it.element === element }
    }
    
    fun captureInput() {
        val childContext = UiElementContext(
            this, null,
            0, 0, this.nav.output.width, this.nav.output.height,
            0, 0, this.nav.output.width, this.nav.output.height
        )
        this.elements.forEach { it.element.captureInput(childContext) }
    }
    
    private var lastOutputWidth: Int = 0
    private var lastOutputHeight: Int = 0
    
    fun update() {
        val outputSizeChanged: Boolean =
            this.nav.output.width != this.lastOutputWidth ||
            this.nav.output.height != this.lastOutputHeight
        if (outputSizeChanged) {
            this.lastOutputWidth = this.nav.output.width
            this.lastOutputHeight = this.nav.output.height
            this.invalidate()
        }
        val childContext = UiElementContext(
            this, null,
            0, 0, this.nav.output.width, this.nav.output.height,
            0, 0, this.nav.output.width, this.nav.output.height
        )
        for (e in this.elements) {
            e.element.update(childContext)
        }
        this.render()
    }
    
    private fun render() {
        val shader: Shader<PxPos, Blit> = blitShader()
        shader[PxPos.bufferSizePx] = Vector2f(
            this.nav.output.width.toFloat(), this.nav.output.height.toFloat()
        )
        shader[PxPos.destTopLeftPx] = Vector2f(0f, 0f)
        for (e in this.elements) {
            val texture: Texture = e.element.result ?: continue
            shader[PxPos.destSizePx] = Vector2f(
                e.element.pxWidth.toFloat(), e.element.pxHeight.toFloat()
            )
            shader[Blit.texture] = texture
            quad().render(
                shader, this.nav.output, depthTesting = DepthTesting.DISABLED
            )
        }
    }
    
    fun invalidate() {
        this.elements.forEach { it.element.invalidate() }
    }

    fun disposeTree() {
        this.elements.forEach { it.element.disposeTree() }
        this.elements.clear()
    }

}