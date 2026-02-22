
package schwalbe.ventura.utils

import org.joml.Vector3fc
import kotlin.math.PI
import kotlin.math.atan2

fun xzVectorAngle(a: Vector3fc, b: Vector3fc): Float = atan2(
    (a.z() * b.x()) - (a.x() * b.z()),
    (a.x() * b.x()) + (a.z() * b.z())
)

fun wrapAngle(angle: Float): Float {
    val fullRot: Float = 2f * PI.toFloat()
    var a = angle
    while (a > +PI) { a -= fullRot }
    while (a < -PI) { a += fullRot }
    return a
}
