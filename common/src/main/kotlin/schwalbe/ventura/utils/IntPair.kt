
package schwalbe.ventura.utils

@JvmInline
value class IntPair(val packed: Long) {

    constructor(tx: Int, tz: Int)
        : this((tx.toLong() shl 32) or (tz.toLong() and 0xFFFFFFFFL))

    val x: Int
        get() = (this.packed shr 32).toInt()

    val z: Int
        get() = (this.packed and 0xFFFFFFFFL).toInt()

}

fun Long.toIntPair(): IntPair = IntPair(this)

