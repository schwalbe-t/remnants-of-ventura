
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Framebuffer
import schwalbe.ventura.engine.gfx.DepthTesting
import schwalbe.ventura.engine.gfx.Shader
import schwalbe.ventura.engine.gfx.Texture
import schwalbe.ventura.engine.input.InputEventQueue
import org.joml.*

class UiContext(
    val output: Framebuffer,
    val input: InputEventQueue,
    defaultFont: Font = Font.default,
    defaultFontSize: UiSize = 16.px,
    defaultFontColor: Vector4fc = Vector4f(0f, 0f, 0f, 1f)
) {
    
    private data class BaseElement(val element: UiElement, val layer: Int)
    
    var defaultFont: Font = defaultFont
        set(value) {
            field = value
            this.invalidate()
        }
    var defaultFontSize: UiSize = defaultFontSize
        set(value) {
            field = value
            this.invalidate()
        }
    var defaultFontColor: Vector4fc = Vector4f(defaultFontColor)
        set(value) {
            field = Vector4f(value)
            this.invalidate()
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
            this,
            this.output.width, this.output.height,
            0, 0
        )
        this.elements.forEach { it.element.captureInput(childContext) }
    }
    
    private var lastOutputWidth: Int = 0
    private var lastOutputHeight: Int = 0
    
    fun update() {
        val outputSizeChanged: Boolean = this.output.width != this.lastOutputWidth
            || this.output.height != this.lastOutputHeight
        if (outputSizeChanged) {
            this.lastOutputWidth = this.output.width
            this.lastOutputHeight = this.output.height
            this.invalidate()
        }
        val childContext = UiElementContext(
            this,
            this.output.width, this.output.height,
            0, 0
        )
        for (e in this.elements) {
            e.element.update(childContext)
        }
        this.render()
    }
    
    private fun render() {
        val shader: Shader<PxPos, Blit> = blitShader()
        shader[PxPos.bufferSizePx] = Vector2f(
            this.output.width.toFloat(), this.output.height.toFloat()
        )
        shader[PxPos.destTopLeftPx] = Vector2f(0f, 0f)
        for (e in this.elements) {
            val texture: Texture = e.element.result ?: continue
            shader[PxPos.destSizePx] = Vector2f(
                e.element.pxWidth.toFloat(), e.element.pxHeight.toFloat()
            )
            shader[Blit.texture] = texture
            quad().render(
                shader, this.output, depthTesting = DepthTesting.DISABLED
            )
        }
    }
    
    fun invalidate() {
        this.elements.forEach { it.element.invalidate() }
    }

}