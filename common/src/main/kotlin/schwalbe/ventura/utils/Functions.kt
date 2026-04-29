
package schwalbe.ventura.utils

import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs

fun sign(i: Int): Int = when {
    i < 0 -> -1
    i > 0 -> +1
    else -> 0
}

fun sign(i: Long): Long = when {
    i < 0 -> -1
    i > 0 -> +1
    else -> 0
}

fun insideSquareRadiusXZ(p: SerVector3, center: SerVector3, r: Float)
    = maxOf(abs(p.x - center.x), abs(p.y - center.y)) <= r

inline fun <T> parseRgbaHex(
    hexColor: String, crossinline f: (Float, Float, Float, Float) -> T
): T {
    val r: Float = if (hexColor.length < 2) 1f
        else hexColor.substring(0, 2).toInt(16).toFloat() / 255f
    val g: Float = if (hexColor.length < 4) 1f
        else hexColor.substring(2, 4).toInt(16).toFloat() / 255f
    val b: Float = if (hexColor.length < 6) 1f
        else hexColor.substring(4, 6).toInt(16).toFloat() / 255f
    val a: Float = if (hexColor.length < 8) 1f
        else hexColor.substring(6, 8).toInt(16).toFloat() / 255f
    return f(r, g, b, a)
}

fun parseRgbaHex(hexColor: String): Vector4f
    = parseRgbaHex(hexColor, ::Vector4f)

fun parseRgbHex(hexColor: String): Vector3f
    = parseRgbaHex(hexColor) { r, g, b, _ -> Vector3f(r, g, b) }
