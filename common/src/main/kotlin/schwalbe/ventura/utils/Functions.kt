
package schwalbe.ventura.utils

import org.joml.Vector4fc
import org.joml.Vector4f
import kotlin.math.abs

fun sign(i: Int): Int = when {
    i < 0 -> -1
    i > 0 -> +1
    else -> 0
}

fun insideSquareRadiusXZ(p: SerVector3, center: SerVector3, r: Float)
    = maxOf(abs(p.x - center.x), abs(p.y - center.y)) <= r

fun parseHexColor(hexColor: String): Vector4fc {
    val r: Float = if (hexColor.length < 2) 1f
        else hexColor.substring(0, 2).toInt(16).toFloat() / 255f
    val g: Float = if (hexColor.length < 4) 1f
        else hexColor.substring(2, 4).toInt(16).toFloat() / 255f
    val b: Float = if (hexColor.length < 6) 1f
        else hexColor.substring(4, 6).toInt(16).toFloat() / 255f
    val a: Float = if (hexColor.length < 8) 1f
        else hexColor.substring(6, 8).toInt(16).toFloat() / 255f
    return Vector4f(r, g, b, a)
}
