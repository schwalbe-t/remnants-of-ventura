
package schwalbe.ventura.engine.ui

import schwalbe.ventura.engine.Resource
import java.awt.Font as AwtFont
import java.io.File
import java.io.IOException

class Font(internal val baseFont: AwtFont) {
    
    companion object {
        val default: Font = Font(
            AwtFont(AwtFont.SANS_SERIF, AwtFont.PLAIN, 32)
        )
    }
    
}

fun Font.Companion.loadTtf(path: String) = Resource<Font> {
    val baseFont: AwtFont
    try {
        baseFont = AwtFont.createFont(AwtFont.TRUETYPE_FONT, File(path))
    } catch (e: IOException) {
        throw IllegalStateException("Font file '$path' could not be read")
    }
    return@Resource { Font(baseFont) }
}