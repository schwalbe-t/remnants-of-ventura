
package schwalbe.ventura.engine

import org.joml.*
import kotlin.math.abs

data class SmoothingOps<T, V>(
    val zero: () -> T,
    val add: (a: T, b: T, dest: T) -> T,
    val sub: (a: T, b: T, dest: T) -> T,
    val scale: (x: T, n: Float, dest: T) -> T,
    val len: (x: T) -> Float,
    val equals: (a: T, b: T) -> Boolean,
    val set: (value: T, dest: T) -> T,
    val toExposed: (x: T) -> V
)

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
        this.current = this.ops.set(this.target, this.current)
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
            this.target, this.current, this.toTarget
        )
        val dist: Float = this.ops.len(this.toTarget)
        if (dist <= this.epsilon) {
            this.current = this.ops.set(this.target, this.current)
            return
        }
        this.toTarget = this.ops.scale(
            this.toTarget, this.response * deltaTime, this.toTarget
        )
        this.current = this.ops.add(
            this.current, this.toTarget, this.current
        )
    }

    fun snapToTarget() {
        this.lastAccessTime = System.nanoTime()
        this.current = this.ops.set(this.target, this.current)
    }

    val isResting: Boolean
        get() = this.ops.equals(this.current, this.target)
    val value: V
        get() = this.ops.toExposed(this.current)
}

typealias SmoothedFloat = Smoothed<Float, Float>
val FLOAT_SMOOTHING_OPS = SmoothingOps<Float, Float>(
    zero = { 0f },
    add = { a, b, _ -> a + b },
    scale = { x, n, _ -> x * n },
    sub = { a, b, _ -> a - b },
    len = ::abs,
    equals = { a, b -> a == b },
    set = { value, _ -> value },
    toExposed = { it }
)
fun Float.smoothed(response: Float, epsilon: Float = 0.1f): SmoothedFloat
    = Smoothed(FLOAT_SMOOTHING_OPS, this, response, epsilon)

typealias SmoothedVector2f = Smoothed<Vector2f, Vector2fc>
val VECTOR2F_SMOOTHING_OPS = SmoothingOps<Vector2f, Vector2fc>(
    zero = { Vector2f(0f, 0f) },
    add = Vector2f::add,
    scale = Vector2f::mul,
    sub = Vector2f::sub,
    len = Vector2f::length,
    equals = Vector2f::equals,
    set = { value, dest -> dest.set(value) },
    toExposed = { it }
)
fun Vector2f.smoothed(response: Float, epsilon: Float = 0.1f): SmoothedVector2f
    = Smoothed(VECTOR2F_SMOOTHING_OPS, this, response, epsilon)

typealias SmoothedVector3f = Smoothed<Vector3f, Vector3fc>
val VECTOR3F_SMOOTHING_OPS = SmoothingOps<Vector3f, Vector3fc>(
    zero = { Vector3f(0f, 0f, 0f) },
    add = Vector3f::add,
    scale = Vector3f::mul,
    sub = Vector3f::sub,
    len = Vector3f::length,
    equals = Vector3f::equals,
    set = { value, dest -> dest.set(value) },
    toExposed = { it }
)
fun Vector3f.smoothed(response: Float, epsilon: Float = 0.1f): SmoothedVector3f
    = Smoothed(VECTOR3F_SMOOTHING_OPS, this, response, epsilon)

typealias SmoothedVector4f = Smoothed<Vector4f, Vector4fc>
val VECTOR4F_SMOOTHING_OPS = SmoothingOps<Vector4f, Vector4fc>(
    zero = { Vector4f(0f, 0f, 0f, 0f) },
    add = Vector4f::add,
    scale = Vector4f::mul,
    sub = Vector4f::sub,
    len = Vector4f::length,
    equals = Vector4f::equals,
    set = { value, dest -> dest.set(value) },
    toExposed = { it }
)
fun Vector4f.smoothed(response: Float, epsilon: Float = 0.1f): SmoothedVector4f
    = Smoothed(VECTOR4F_SMOOTHING_OPS, this, response, epsilon)