
package schwalbe.ventura.engine

import org.joml.*
import kotlin.math.abs

interface SmoothingOps<T, V> {
    fun zero(): T
    fun add(a: T, b: T, dest: T): T
    fun sub(a: T, b: T, dest: T): T
    fun scale(x: T, n: Float, dest: T): T
    fun len(x: T): Float
    fun equals(a: T, b: T): Boolean
    fun set(value: T, dest: T): T
    fun toExposed(x: T): V
}

private const val SECS_PER_NANO: Double = 1.0 / 1_000_000_000.0

class Smoothed<T, V>(
    val ops: SmoothingOps<T, V>,
    initialValue: T,
    var response: Float,
    var epsilon: Float
) {

    var target: T = initialValue

    private var toTarget: T = ops.zero()
    private var current: T = ops.zero()

    init {
        this.current = this.ops.set(value = this.target, dest = this.current)
    }

    private var lastAccessTime: Long = System.nanoTime()
    private fun computeDeltaTime(): Float {
        val now: Long = System.nanoTime()
        val dtNanos: Long = now - this.lastAccessTime
        val dtSecs: Float = (dtNanos.toDouble() * SECS_PER_NANO).toFloat()
        this.lastAccessTime = now
        return dtSecs
    }

    fun update() {
        val deltaTime: Float = this.computeDeltaTime()
        this.toTarget = this.ops.sub(
            a = this.target, b = this.current, dest = this.toTarget
        )
        val dist: Float = this.ops.len(this.toTarget)
        if (dist <= this.epsilon) {
            this.current = this.ops.set(
                value = this.target, dest = this.current
            )
            return
        }
        val moveDist: Float = minOf(this.response * deltaTime, 1f)
        this.toTarget = this.ops.scale(
            x = this.toTarget, n = moveDist, dest = this.toTarget
        )
        this.current = this.ops.add(
            a = this.current, b = this.toTarget, dest = this.current
        )
    }

    fun snapToTarget() {
        this.lastAccessTime = System.nanoTime()
        this.current = this.ops.set(value = this.target, dest = this.current)
    }

    val isResting: Boolean
        get() = this.ops.equals(a = this.current, b = this.target)
    val value: V
        get() = this.ops.toExposed(this.current)
}

typealias SmoothedFloat = Smoothed<Float, Float>
object FloatSmoothingOps : SmoothingOps<Float, Float> {
    override fun zero(): Float
        = 0f
    override fun add(a: Float, b: Float, dest: Float): Float
        = a + b
    override fun scale(x: Float, n: Float, dest: Float): Float
        = x * n
    override fun sub(a: Float, b: Float, dest: Float): Float
        = a - b
    override fun len(x: Float): Float
        = abs(x)
    override fun equals(a: Float, b: Float): Boolean
        = a == b
    override fun set(value: Float, dest: Float): Float
        = value
    override fun toExposed(x: Float): Float
        = x
}
fun Float.smoothed(response: Float, epsilon: Float = 0.1f): SmoothedFloat
    = Smoothed(FloatSmoothingOps, this, response, epsilon)

typealias SmoothedVector2f = Smoothed<Vector2f, Vector2fc>
object Vector2fSmoothingOps : SmoothingOps<Vector2f, Vector2fc> {
    override fun zero(): Vector2f
        = Vector2f(0f, 0f)
    override fun add(a: Vector2f, b: Vector2f, dest: Vector2f): Vector2f
        = a.add(b, dest)
    override fun scale(x: Vector2f, n: Float, dest: Vector2f): Vector2f
        = x.mul(n, dest)
    override fun sub(a: Vector2f, b: Vector2f, dest: Vector2f): Vector2f
        = a.sub(b, dest)
    override fun len(x: Vector2f): Float
        = x.length()
    override fun equals(a: Vector2f, b: Vector2f): Boolean
        = a == b
    override fun set(value: Vector2f, dest: Vector2f): Vector2f
        = dest.set(value)
    override fun toExposed(x: Vector2f): Vector2fc
        = x
}
fun Vector2f.smoothed(response: Float, epsilon: Float = 0.1f): SmoothedVector2f
    = Smoothed(Vector2fSmoothingOps, this, response, epsilon)

typealias SmoothedVector3f = Smoothed<Vector3f, Vector3fc>
object Vector3fSmoothingOps : SmoothingOps<Vector3f, Vector3fc> {
    override fun zero(): Vector3f
        = Vector3f(0f, 0f, 0f)
    override fun add(a: Vector3f, b: Vector3f, dest: Vector3f): Vector3f
        = a.add(b, dest)
    override fun scale(x: Vector3f, n: Float, dest: Vector3f): Vector3f
        = x.mul(n, dest)
    override fun sub(a: Vector3f, b: Vector3f, dest: Vector3f): Vector3f
        = a.sub(b, dest)
    override fun len(x: Vector3f): Float
        = x.length()
    override fun equals(a: Vector3f, b: Vector3f): Boolean
        = a == b
    override fun set(value: Vector3f, dest: Vector3f): Vector3f
        = dest.set(value)
    override fun toExposed(x: Vector3f): Vector3fc
        = x
}
fun Vector3f.smoothed(response: Float, epsilon: Float = 0.1f): SmoothedVector3f
    = Smoothed(Vector3fSmoothingOps, this, response, epsilon)

typealias SmoothedVector4f = Smoothed<Vector4f, Vector4fc>
object Vector4fSmoothingOps : SmoothingOps<Vector4f, Vector4fc> {
    override fun zero(): Vector4f
        = Vector4f(0f, 0f, 0f, 0f)
    override fun add(a: Vector4f, b: Vector4f, dest: Vector4f): Vector4f
        = a.add(b, dest)
    override fun scale(x: Vector4f, n: Float, dest: Vector4f): Vector4f
        = x.mul(n, dest)
    override fun sub(a: Vector4f, b: Vector4f, dest: Vector4f): Vector4f
        = a.sub(b, dest)
    override fun len(x: Vector4f): Float
        = x.length()
    override fun equals(a: Vector4f, b: Vector4f): Boolean
        = a == b
    override fun set(value: Vector4f, dest: Vector4f): Vector4f
        = dest.set(value)
    override fun toExposed(x: Vector4f): Vector4fc
        = x
}
fun Vector4f.smoothed(response: Float, epsilon: Float = 0.1f): SmoothedVector4f
    = Smoothed(Vector4fSmoothingOps, this, response, epsilon)