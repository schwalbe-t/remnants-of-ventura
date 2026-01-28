
package schwalbe.ventura.engine

import org.joml.Matrix4fc
import org.joml.Vector3f
import org.joml.Vector3fc


private fun Vector3fc.allLessEq(p: Vector3fc): Boolean
    = this.x() <= p.x() && this.y() <= p.y() && this.z() <= p.z()

private fun minOf(a: Vector3fc, b: Vector3fc, dest: Vector3f): Vector3f
    = dest.set(minOf(a.x(), b.x()), minOf(a.y(), b.y()), minOf(a.z(), b.z()))

private fun maxOf(a: Vector3fc, b: Vector3fc, dest: Vector3f): Vector3f
    = dest.set(maxOf(a.x(), b.x()), maxOf(a.y(), b.y()), maxOf(a.z(), b.z()))


interface AxisAlignedBox {
    val min: Vector3fc
    val max: Vector3fc
}

operator fun AxisAlignedBox.contains(p: Vector3fc): Boolean
    = this.min.allLessEq(p) && p.allLessEq(this.max)

operator fun AxisAlignedBox.contains(b: AxisAlignedBox): Boolean
    = this.min.allLessEq(b.min) && b.max.allLessEq(this.max)

fun AxisAlignedBox.intersects(b: AxisAlignedBox): Boolean
    = this.min.allLessEq(b.max) && b.min.allLessEq(this.max)


class ConstAxisAlignedBox(
    override val min: Vector3fc,
    override val max: Vector3fc
) : AxisAlignedBox

fun axisBoxOf(a: Vector3fc, b: Vector3fc): AxisAlignedBox
    = ConstAxisAlignedBox(minOf(a, b, Vector3f()), maxOf(a, b, Vector3f()))

fun AxisAlignedBox.toAxisBox(): AxisAlignedBox
    = ConstAxisAlignedBox(Vector3f(this.min), Vector3f(this.max))

fun AxisAlignedBox.translate(offset: Vector3fc): AxisAlignedBox
    = ConstAxisAlignedBox(
        this.min.add(offset, Vector3f()),
        this.max.add(offset, Vector3f())
    )


private const val NEG_INF: Float = Float.NEGATIVE_INFINITY
private const val POS_INF: Float = Float.POSITIVE_INFINITY

class MutableAxisAlignedBox(
    override val min: Vector3f = Vector3f(POS_INF, POS_INF, POS_INF),
    override val max: Vector3f = Vector3f(NEG_INF, NEG_INF, NEG_INF)
) : AxisAlignedBox

fun MutableAxisAlignedBox.add(p: Vector3fc): MutableAxisAlignedBox {
    minOf(this.min, p, dest = this.min)
    maxOf(this.max, p, dest = this.max)
    return this
}

fun MutableAxisAlignedBox.transform(t: Matrix4fc): MutableAxisAlignedBox {
    val oldMin = Vector3f(this.min)
    val oldMax = Vector3f(this.max)
    this.min.set(POS_INF, POS_INF, POS_INF)
    this.max.set(NEG_INF, NEG_INF, NEG_INF)
    val p = Vector3f()
    for (x in listOf(oldMin.x(), oldMax.x())) {
        for (y in listOf(oldMin.y(), oldMax.y())) {
            for (z in listOf(oldMin.z(), oldMax.z())) {
                p.set(x, y, z)
                t.transformPosition(p)
                this.add(p)
            }
        }
    }
    return this
}

fun AxisAlignedBox.toMutableAxisBox(): MutableAxisAlignedBox
    = MutableAxisAlignedBox(Vector3f(this.min), Vector3f(this.max))
