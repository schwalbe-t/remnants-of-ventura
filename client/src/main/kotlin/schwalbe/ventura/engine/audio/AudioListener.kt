
package schwalbe.ventura.engine.audio

import org.joml.Vector3f
import org.lwjgl.openal.AL10.*
import org.joml.Vector3fc

object AudioListener {

    var gain: Float = 1f
        set(value) {
            field = value
            alListenerf(AL_GAIN, value)
        }

    var pitch: Float = 1f
        set(value) {
            field = value
            alListenerf(AL_PITCH, value)
        }

    private val lastPosition = Vector3f()
    var position: Vector3fc
        get() = this.lastPosition
        set(v) {
            this.lastPosition.set(v)
            alListener3f(AL_POSITION, v.x(), v.y(), v.z())
        }

    private val lastOrientation = FloatArray(6) { 0f }
    private val lastForward = Vector3f()
    val forward: Vector3fc = this.lastForward
    private val lastUp = Vector3f()
    val up: Vector3fc = this.lastUp

    fun setLookAlong(forward: Vector3fc, up: Vector3fc) {
        this.lastOrientation[0] = forward.x()
        this.lastOrientation[1] = forward.y()
        this.lastOrientation[2] = forward.z()
        this.lastForward.set(forward)
        this.lastOrientation[3] = up.x()
        this.lastOrientation[4] = up.y()
        this.lastOrientation[5] = up.z()
        this.lastUp.set(up)
        alListenerfv(AL_ORIENTATION, this.lastOrientation)
    }

    fun setLookAt(pos: Vector3fc, at: Vector3fc, up: Vector3fc) {
        val forward: Vector3f = Vector3f(at).sub(pos)
        this.position = pos
        this.setLookAlong(forward, up)
    }

}
