
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Shader
import schwalbe.ventura.engine.gfx.Texture
import org.joml.Vector2f
import kotlin.math.roundToInt

open class Axis(
    val dirX: Int,    val dirY: Int,
    val orthoX: Int,  val orthoY: Int
) : GpuUiElement() {
    
    companion object {
        fun row()                       = Axis(1, 0, 0, 1)
        fun row(defaultSize: UiSize)    = SizedAxis(defaultSize, 1, 0, 0, 1)
        fun column()                    = Axis(0, 1, 1, 0)
        fun column(defaultSize: UiSize) = SizedAxis(defaultSize, 0, 1, 1, 0)
    }
    
    private data class Entry(
        val elem: UiElement, val size: UiSize,
        var elemCtx: UiElementContext? = null
    )
    
    
    private var inside: MutableList<Entry> = mutableListOf()
    
    override val children: List<UiElement>
        get() = this.inside.map(Entry::elem)
    
    override fun updateLayout(context: UiElementContext) {
        var offset = 0
        for (entry in this.inside) {
            val size: Int = maxOf(entry.size(context).roundToInt(), 0)
            val innerRelPxX: Int = dirX * offset
            val innerRelPXY: Int = dirY * offset
            val innerWidth: Int = dirX * size + orthoX * context.parentPxWidth
            val innerHeight: Int = dirY * size + orthoY * context.parentPxHeight
            entry.elemCtx = context.childContext(
                this, innerRelPxX, innerRelPXY, innerWidth, innerHeight
            )
            offset += size
        }
        this.pxWidth = dirX * offset + orthoX * context.parentPxWidth
        this.pxHeight = dirY * offset + orthoY * context.parentPxHeight
    }
        
    override fun updateChildren(
        context: UiElementContext, f: (UiElement, UiElementContext) -> Unit
    ) {
        for (entry in this.inside) {
            val childContext: UiElementContext = entry.elemCtx ?: continue
            f(entry.elem, childContext)
        }
    }
        
    override fun render(context: UiElementContext) {
        this.prepareTarget()
        val shader: Shader<PxPos, Blit> = blitShader()
        shader[PxPos.bufferSizePx] = Vector2f(
            this.target.width.toFloat(), this.target.height.toFloat()
        )
        for (entry in this.inside) {
            val elemTex: Texture = entry.elem.result ?: continue
            val elemCtx: UiElementContext = entry.elemCtx ?: continue
            val relPxX: Int = elemCtx.absPxX - context.absPxX
            val relPxY: Int = elemCtx.absPxY - context.absPxY
            shader[PxPos.destTopLeftPx] = Vector2f(
                relPxX.toFloat(), relPxY.toFloat()
            )
            shader[PxPos.destSizePx] = Vector2f(
                entry.elem.pxWidth.toFloat(), entry.elem.pxHeight.toFloat()
            )
            shader[Blit.texture] = elemTex
            quad().render(shader, this.target)
        }
    }
    
    open fun add(size: UiSize, element: UiElement): Axis {
        this.inside.add(Entry(element, size))
        this.invalidate()
        return this
    }
    
    fun map(f: (UiElement) -> UiElement): Axis {
        this.inside = this.inside
            .map { e -> Entry(f(e.elem), e.size) }
            .toMutableList()
        this.invalidate()
        return this
    }
    
}

class SizedAxis(
    val defaultSize: UiSize,
    dirX: Int, dirY: Int, orthoX: Int, orthoY: Int
) : Axis(dirX, dirY, orthoX, orthoY) {
    
    override fun add(size: UiSize, element: UiElement): SizedAxis {
        super.add(size, element)
        return this
    }
    
    fun add(element: UiElement): SizedAxis
        = this.add(this.defaultSize, element)
    
}