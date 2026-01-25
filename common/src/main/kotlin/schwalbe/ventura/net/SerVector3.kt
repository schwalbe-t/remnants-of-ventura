package schwalbe.ventura.net

import kotlinx.serialization.Serializable
import org.joml.Vector3f
import org.joml.Vector3fc

@Serializable
data class SerVector3(val x: Float, val y: Float, val z: Float) {
    companion object
}

fun SerVector3.Companion.fromVector3f(v: Vector3fc)
    = SerVector3(v.x(), v.y(), v.z())

fun SerVector3.toVector3f(): Vector3f
    = Vector3f(this.x, this.y, this.z)

fun SerVector3.storeInto(dest: Vector3f): Vector3f {
    dest.set(this.x, this.y, this.z)
    return dest
}