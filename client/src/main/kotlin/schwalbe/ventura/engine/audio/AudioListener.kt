
package schwalbe.ventura.engine.audio

import org.joml.Vector3f
import org.lwjgl.openal.AL10.*
import org.joml.Vector3fc

object AudioListener {

    fun setPosition(pos: Vector3fc) {
        alListener3f(AL_POSITION, pos.x(), pos.y(), pos.z())
    }

    private val lastOrientation = FloatArray(6) { 0f }

    fun setLookAlong(forward: Vector3fc, up: Vector3fc) {
        this.lastOrientation[0] = forward.x()
        this.lastOrientation[1] = forward.y()
        this.lastOrientation[2] = forward.z()
        this.lastOrientation[3] = up.x()
        this.lastOrientation[4] = up.y()
        this.lastOrientation[5] = up.z()
        alListenerfv(AL_ORIENTATION, this.lastOrientation)
    }

    fun setLookAt(pos: Vector3fc, at: Vector3fc, up: Vector3fc) {
        val forward: Vector3f = Vector3f(at).sub(pos)
        this.setPosition(pos)
        this.setLookAlong(forward, up)
    }

}
