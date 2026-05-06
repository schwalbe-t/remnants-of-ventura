
package schwalbe.ventura

import schwalbe.ventura.utils.SerVector3
import schwalbe.ventura.utils.parseRgbHex
import schwalbe.ventura.utils.toSerVector3
import kotlinx.serialization.Serializable
import org.joml.Vector3fc

@Serializable
enum class PaletteColor(val hex: String) {
    BLACK           ("443331"),
    DARK_BROWN      ("50473f"),
    BROWN           ("705448"),
    DARK_GRAY       ("6e7261"),
    GRAY            ("97866f"),
    BRIGHT_GRAY     ("a8ac89"),
    WHITE           ("d1c19e"),
    BRIGHT_BLUE     ("9ba9a0"),
    BLUE            ("8a97a1"),
    PURPLE          ("816891"),
    MAGENTA         ("aa749e"),
    PINK            ("cb8993"),
    CREAM           ("d4a488"),
    YELLOW          ("d2ad72"),
    BRIGHT_ORANGE   ("d3925b"),
    ORANGE          ("cc785b"),
    RED             ("ba5e69"),
    DARK_RED        ("94554d"),
    WINE            ("784a5d"),
    DARK_BLUE       ("525979"),
    DARK_GREEN      ("437f5d"),
    CYAN            ("5a8b97"),
    GREEN           ("86a063"),
    AQUA            ("85b69a");

    val norm: Vector3fc = parseRgbHex(this.hex)
    val ser: SerVector3 = this.norm.toSerVector3()
}
