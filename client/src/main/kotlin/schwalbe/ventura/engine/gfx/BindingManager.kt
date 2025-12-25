
package schwalbe.ventura.engine.gfx

/**
 * Class responsible for managing various bindable types.
 * A "bindable" type is anything that needs to be "bound" before using it,
 * and of which only one can be bound at a time.
 * This class makes sure that each bindable type can be used multiple times
 * while having to bind it as few times as possible (since binding may be
 * an expensive operation).
 * 
 * For example, multiple rendering operations may want to render using the same
 * shader, for which the shader needs to be bound.
 * We could have operation #1 bind the shader and then have operation #2 not
 * bind it again (since we know that operation #1 has already bound it), but
 * that would be messy if the different operations are in entirely different
 * parts of the code and may change in the future.
 * Instead we can use this class: Operation #1 binds the shader once, and the
 * subsequent call to [bindLazy] by operation #2 will then NOT bind the shader
 * again, since the manager remembers that the same shader was already bound.
 */
internal class BindingManager<T>(bindImpl: (T) -> Unit) {

    private val bind: (T) -> Unit = bindImpl
    
    var last: T? = null
        private set

    /**
     * Always binds the given `thing`, even if it is known to be the last thing
     * that was bound. This is useful when you know that `thing` is not already
     * bound, since this method does not involve any checks.
     */
    fun bindEager(thing: T) {
        this.last = thing
        this.bind(thing)
    }

    /**
     * Binds the given `thing` unless it is known to be the last thing that was
     * bound.
     * Use [invalidate] or [invalidateAll] to make this manager "forget" about
     * the last bound instance, forcing the next call to this method
     * to always bind.
     * Use [bindEager] to always bind the given instance, no matter what
     * instance was last bound.
     */
    fun bindLazy(thing: T) {
        val last: T? = this.last
        if (last != null && thing == last) { return }
        this.last = thing
        this.bind(thing)
    }

    /** 
     * Makes this manager forget about the given `thing` being the last bound
     * instance, if that is the case.
     * This is useful when any binding of `thing` has been invalidated,
     * since even a call to [bindLazy] will need to re-bind the given `thing`.
     */
    fun invalidate(thing: T) {
        val last: T = this.last ?: return
        if (last != thing) { return }
        this.last = null
    }
    
    /**
     * Makes this manager forget about any instance other than the given `thing`
     * being the last thing bound, if that is the case.
     * This is useful when any binding other than `thing` has been invalidated,
     * since even a call to [bindLazy] will need to re-bind any other instance.
     */
    fun invalidateUnless(thing: T) {
        val last: T = this.last ?: return
        if (last == thing) { return }
        this.last = null
    }

    /**
     * Makes this manager forget about any instance that was last bound.
     * This is useful when any binding of this type has been invalidated,
     * since any call to [bindLazy] will need to re-bind the given `thing`.
     */
     fun invalidateAll() {
        this.last = null
     }

}