
package schwalbe.ventura.engine.gfx

import org.joml.*
import kotlin.math.roundToInt

data class AnimationRef<A : Animations<A>>(
    val name: String, val speed: Float
) {
    fun withSpeed(speed: Float) = AnimationRef<A>(this.name, speed)
}

interface Animations<A : Animations<A>> {
    fun anim(name: String, speed: Float = 1f) = AnimationRef<A>(name, 1f)
}

class Animation<A : Animations<A>>(
    val name: String,
    val lengthSecs: Float,
    val channels: Map<String, Channel>
) {
    
    data class KeyFrame<T>(
        val time: Float,
        val value: T
    )
    
    data class ChannelValue(
        val position: Vector3fc,
        val rotation: Quaternionfc,
        val scale: Vector3fc
    ) {
        companion object
    }
    
    data class Channel(
        val position: List<KeyFrame<Vector3fc>>,
        val rotation: List<KeyFrame<Quaternionfc>>,
        val scale: List<KeyFrame<Vector3fc>>
    )
    
}

private fun <V, D : V> List<Animation.KeyFrame<V>>.interpolate(
    progressSecs: Float,
    default: () -> D,
    interpolate: (V, V, Float, D) -> V,
): V {
    val beforeI: Int = this.indexOfLast { it.time <= progressSecs }
    if (beforeI == -1) { return default() }
    val before: Animation.KeyFrame<V> = this[beforeI]
    val after: Animation.KeyFrame<V> = this.getOrNull(beforeI + 1)
        ?: return before.value
    val frameDelta: Float = after.time - before.time
    if (frameDelta == 0f) { return before.value }
    val t: Float = (progressSecs - before.time) / frameDelta
    return interpolate(before.value, after.value, t, default())
}

fun Animation.Channel.interpolate(
    progressSecs: Float
): Animation.ChannelValue {
    val position: Vector3fc = this.position
        .interpolate(progressSecs, ::Vector3f, Vector3fc::lerp)
    val rotation: Quaternionfc = this.rotation
        .interpolate(progressSecs, ::Quaternionf, Quaternionfc::slerp)
    val scale: Vector3fc = this.scale
        .interpolate(progressSecs, ::Vector3f, Vector3fc::lerp)
    return Animation.ChannelValue(position, rotation, scale)
}

fun Animation.ChannelValue.Companion.interpolate(
    a: Animation.ChannelValue, b: Animation.ChannelValue, t: Float
): Animation.ChannelValue {
    val position: Vector3f      = a.position.lerp(b.position, t, Vector3f())
    val rotation: Quaternionf   = a.rotation.slerp(b.rotation, t, Quaternionf())
    val scale: Vector3f         = a.scale.lerp(b.scale, t, Vector3f())
    return Animation.ChannelValue(position, rotation, scale)
}

fun Animation.ChannelValue.toTransform(): Matrix4f = Matrix4f()
    .translate(this.position)
    .rotate(this.rotation)
    .scale(this.scale)

class AnimState<A : Animations<A>>(initial: AnimationRef<A>) {
    
    class Entry<A : Animations<A>>(val anim: AnimationRef<A>) {
        var progressSecs: Float = 0f
            private set
        
        fun addTimePassed(deltaTime: Float) {
            this.progressSecs += deltaTime
        }
    }
    
    data class Transition<A : Animations<A>>(
        val entry: Entry<A>,
        val duration: Float
    ) {
        var progressFrac: Float = 0f
            private set
        
        val hasEnded: Boolean
            get() = this.progressFrac >= 1f
            
        fun addTimePassed(deltaTime: Float): Float {
            val remTimeSecs: Float = (1f - this.progressFrac) * this.duration
            val addedProgressSecs: Float = minOf(deltaTime, remTimeSecs)
            this.progressFrac += addedProgressSecs / this.duration
            return deltaTime - addedProgressSecs
        }
    }
    
    var current: Entry<A> = Entry(initial)
        private set
    
    private val transitionQueue: MutableList<Transition<A>> = mutableListOf()
    val transition: Transition<A>?
        get() = this.transitionQueue.getOrNull(0)
    val isTransitioning: Boolean
        get() = this.transition != null
    
    fun transitionTo(
        anim: AnimationRef<A>, transitionSecs: Float = 0.0f
    ) {
        this.transitionQueue.add(Transition(Entry(anim), transitionSecs))
    }
    
    private fun updateEntries(deltaTime: Float) {
        this.current.addTimePassed(deltaTime)
        this.transitionQueue.forEach {
            it.entry.addTimePassed(deltaTime)
        }
    }
    
    private fun updateTransitions(deltaTime: Float) {
        var remTransTime: Float = deltaTime
        while (remTransTime > 0f) {
            val trans: Transition<A> = this.transition ?: break
            remTransTime = trans.addTimePassed(remTransTime)
            if (trans.hasEnded) {
                this.current = this.transitionQueue.removeFirst().entry
            }
        }
    }
    
    fun addTimePassed(deltaTime: Float) {
        this.updateEntries(deltaTime)
        this.updateTransitions(deltaTime)
    }
    
}

private fun <A : Animations<A>> AnimState<A>.channelValue(
    model: Model<A>, name: String
): Animation.ChannelValue? {
    val refA: AnimationRef<A> = this.current.anim
    val animA: Animation<A> = model.animations[refA.name]
        ?: return null
    val timeA: Float
        = (this.current.progressSecs * refA.speed) % animA.lengthSecs
    val valA: Animation.ChannelValue = animA.channels[name]
        ?.interpolate(timeA)
        ?: return null
    val transition: AnimState.Transition<A> = this.transition
        ?: return valA
    val refB: AnimationRef<A> = transition.entry.anim
    val animB: Animation<A> = model.animations[refB.name]
        ?: return valA
    val timeB: Float
        = (transition.entry.progressSecs * refB.speed) % animB.lengthSecs
    val valB: Animation.ChannelValue = animB.channels[name]
        ?.interpolate(timeB)
        ?: return valA
    return Animation.ChannelValue
        .interpolate(valA, valB, transition.progressFrac)
}

private fun <A : Animations<A>> AnimState<A>.collectNodeTransforms(
    model: Model<A>, node: Model.Node, parentTransform: Matrix4fc,
    out: MutableMap<String, Matrix4fc>
) {
    val transform: Matrix4f
        = this.channelValue(model, node.name)?.toTransform()
        ?: Matrix4f(node.localTransform)
    parentTransform.mul(transform, transform)
    out[node.name] = transform
    node.children.forEach {
        this.collectNodeTransforms(model, it, transform, out)
    }
}

fun <A : Animations<A>> AnimState<A>.computeJointTransforms(
    model: Model<A>
): Map<String, Matrix4fc> {
    val out = mutableMapOf<String, Matrix4fc>()
    val root: Model.Node = model.rootNode ?: return out
    this.collectNodeTransforms(model, root, Matrix4f(), out)
    return out
}