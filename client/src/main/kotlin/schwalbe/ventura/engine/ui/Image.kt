
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.Texture
import kotlin.math.roundToInt

class Image : GpuUiElement() {

    var image: Texture? = null
    var imageWidth: UiSize = 100.pw
    var imageHeight: UiSize = 100.ph
    
    override val children: List<UiElement> = listOf()

    override fun render(context: UiElementContext) {
        val image: Texture = this.image ?: return
        this.prepareTarget()
        val destW: Int = this.imageWidth(context).roundToInt()
        val destH: Int = this.imageHeight(context).roundToInt()
        blitTexture(
            image, this.target, 0, 0, destW, destH, preMultiplyAlpha = true
        )
    }

    fun withImage(
        image: Texture?, width: UiSize = 100.pw, height: UiSize = 100.ph
    ): Image {
        this.image = image
        this.imageWidth = width
        this.imageHeight = height
        this.invalidate()
        return this
    }
    
}