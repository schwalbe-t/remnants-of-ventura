
package schwalbe.ventura.utils

import schwalbe.ventura.net.SerVector3
import kotlin.math.abs

fun sign(i: Int): Int = when {
    i < 0 -> -1
    i > 0 -> +1
    else -> 0
}

fun insideSquareRadiusXZ(p: SerVector3, center: SerVector3, r: Float)
        = maxOf(abs(p.x - center.x), abs(p.y - center.y)) <= r
