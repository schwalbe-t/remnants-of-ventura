
package schwalbe.ventura.utils

fun sign(i: Int): Int = when {
    i < 0 -> -1
    i > 0 -> +1
    else -> 0
}
