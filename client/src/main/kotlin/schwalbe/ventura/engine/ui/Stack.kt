
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Shader
import schwalbe.ventura.engine.gfx.Texture
import org.joml.Vector2f

class Stack : GpuUiElement() {
    
    private var inside: MutableList<UiElement> = mutableListOf()
    
    override val children: List<UiElement>
        get() = this.inside
        
    override fun render(context: UiElementContext) {
        this.prepareTarget()
        val shader: Shader<PxPos, Blit> = blitShader()
        shader[PxPos.bufferSizePx] = Vector2f(
            this.target.width.toFloat(), this.target.height.toFloat()
        )
        shader[PxPos.destTopLeftPx] = Vector2f(0f, 0f)
        for (e in this.inside) {
            val eTex: Texture = e.result ?: continue
            shader[PxPos.destSizePx] = Vector2f(
                eTex.width.toFloat(), eTex.height.toFloat()
            )
            shader[Blit.texture] = eTex
            quad().render(shader, this.target)
        }
    }
    
    fun withContents(vararg inside: UiElement): Stack {
        this.inside = inside.toMutableList()
        this.invalidate()
        return this
    }
    
    fun withoutContents(): Stack {
        this.inside = mutableListOf()
        this.invalidate()
        return this
    }

    fun setContents(inside: Iterable<UiElement>) {
        this.inside = inside.toMutableList()
        this.invalidate()
    }

}