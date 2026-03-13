
package schwalbe.ventura.utils

import schwalbe.ventura.net.SerVector3
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File

class GroundColorReader(textureFile: File) {

    val image: BufferedImage = ImageIO.read(textureFile)
    val centerX: Int = this.image.width / 2
    val centerY: Int = this.image.height / 2

    operator fun get(chunkX: Int, chunkZ: Int): SerVector3 {
        val px: Int = (this.centerX + chunkX).coerceIn(0, this.image.width - 1)
        val py: Int = (this.centerY + chunkZ).coerceIn(0, this.image.height - 1)
        val rgb: Int = this.image.getRGB(px, py)
        val r: Int = (rgb shr 16) and 0xFF
        val g: Int = (rgb shr 8) and 0xFF
        val b: Int = rgb and 0xFF
        return SerVector3(
            r.toFloat() / 255f, g.toFloat() / 255f, b.toFloat() / 255f
        )
    }

}
