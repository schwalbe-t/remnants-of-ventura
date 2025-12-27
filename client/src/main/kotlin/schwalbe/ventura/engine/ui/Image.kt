
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Texture

class Image : UiElement() {    
    
    val imageWidth: UiSize = { this.result?.width?.toFloat() ?: 0f }
    val imageHeight: UiSize = { this.result?.height?.toFloat() ?: 0f }
    
    init {
        this.width = this.imageWidth
        this.height = this.imageHeight
    }
    
    override val children: List<UiElement> = listOf()
    
    fun withImage(image: Texture?): Image {
        this.result = image
        this.invalidate()
        return this
    }
    
}