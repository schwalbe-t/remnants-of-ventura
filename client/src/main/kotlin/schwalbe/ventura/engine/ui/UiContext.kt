
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.gfx.ConstFramebuffer

class UiContext(
    val output: ConstFramebuffer,
    val defaultFontFamily: String,
    val defaultFontSize: UiSize
) {

    fun update() {
        // TODO! update all
    }
    
    fun render() {
        // TODO! blit the textures of each base container onto the framebuffer
    }

}