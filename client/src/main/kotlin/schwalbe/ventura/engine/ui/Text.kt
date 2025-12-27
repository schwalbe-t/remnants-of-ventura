
package schwalbe.ventura.engine.ui

class Text : UiElement() {
    
    var value: String = ""
        private set
        
    var fontFamily: String? = null
        private set
    var fontSize: UiSize? = null
        private set
    
    override val children: List<UiElement> = listOf()
    
    override fun render(context: UiElementContext) {
        val fontFamily: String = this.fontFamily
            ?: context.global.defaultFontFamily
        val fontSize: UiSize = this.fontSize
            ?: context.global.defaultFontSize
        val fontSizePx: Float = fontSize(context)
        // TODO! render text onto 'this.result' using Java built-in font stack
        // stay inside of bounds specified by "this.pxWidth" and "this.pxHeight"
    }
    
    fun withText(text: String): Text {
        this.value = text
        this.invalidate()
        return this
    }
    
    fun withFontFamily(family: String?): Text {
        this.fontFamily = family
        this.invalidate()
        return this
    }
    
    fun withFontSize(size: UiSize?): Text {
        this.fontSize = size
        this.invalidate()
        return this
    }
    
}