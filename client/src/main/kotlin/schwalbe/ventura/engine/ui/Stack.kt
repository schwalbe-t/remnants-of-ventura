
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
                e.pxWidth.toFloat(), e.pxHeight.toFloat()
            )
            shader[Blit.texture] = eTex
            quad().render(shader, this.target)
        }
    }
    
    fun add(element: UiElement): Stack {
        this.inside.add(element)
        this.invalidate()
        return this
    }

    fun dispose(element: UiElement): Stack {
        element.disposeTree()
        return this.detach(element)
    }

    fun disposeAll(): Stack {
        this.inside.forEach(UiElement::disposeTree)
        return this.detachAll()
    }

    fun detach(element: UiElement): Stack {
        this.inside.remove(element)
        this.invalidate()
        return this
    }

    fun detachAll(): Stack {
        this.inside.clear()
        this.invalidate()
        return this
    }
    
    fun map(f: (UiElement) -> UiElement): Stack {
        this.inside = this.inside
            .map(f)
            .toMutableList()
        this.invalidate()
        return this
    }

}