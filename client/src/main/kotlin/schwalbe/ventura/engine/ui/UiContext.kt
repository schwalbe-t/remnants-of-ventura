
package schwalbe.ventura.engine.ui

import org.joml.Vector2f
import schwalbe.ventura.engine.gfx.ConstFramebuffer
import schwalbe.ventura.engine.gfx.DepthTesting
import schwalbe.ventura.engine.gfx.Shader
import schwalbe.ventura.engine.gfx.Texture

class UiContext(
    val output: ConstFramebuffer,
    val defaultFontFamily: String,
    val defaultFontSize: UiSize
) {
    
    private data class BaseElement(val element: UiElement, val layer: Int)
    
    private val elements: MutableList<BaseElement> = mutableListOf()
    
    fun add(element: UiElement, layer: Int = 0) {
        val index: Int = this.elements
            .binarySearch { it.layer.compareTo(layer) }
        val insertAt: Int = if (index >= 0) { index } else { -index - 1 }
        this.elements.add(insertAt, BaseElement(element, layer))
    }
    
    fun remove(element: UiElement) {
        this.elements.removeIf { it.element === element }
    }
    
    private var lastOutputWidth: Int = 0
    private var lastOutputHeight: Int = 0
    private var childContext: UiElementContext
        = UiElementContext(this, UiParentContext(0f, 0f))
    
    fun update() {
        val outputSizeChanged: Boolean = this.output.width != this.lastOutputWidth
            || this.output.height != this.lastOutputHeight
        if (outputSizeChanged) {
            this.lastOutputWidth = this.output.width
            this.lastOutputHeight = this.output.height
            val parentContext = UiParentContext(
                this.output.width.toFloat(),
                this.output.height.toFloat()
            )
            this.childContext = UiElementContext(this, parentContext)
            this.elements.forEach { it.element.invalidate() }
        }
        for (e in this.elements) {
            e.element.update(this.childContext)
        }
    }
    
    fun render() {
        val shader: Shader<PxPos, Blit> = blitShader()
        shader[PxPos.bufferSizePx] = Vector2f(
            this.output.width.toFloat(), this.output.height.toFloat()
        )
        shader[PxPos.destTopLeftPx] = Vector2f(0f, 0f)
        for (e in this.elements) {
            val texture: Texture = e.element.result ?: continue
            shader[PxPos.destSizePx] = Vector2f(
                texture.width.toFloat(), texture.height.toFloat()
            )
            shader[Blit.texture] = texture
            quad().render(
                shader, this.output, depthTesting = DepthTesting.DISABLED
            )
        }
    }

}