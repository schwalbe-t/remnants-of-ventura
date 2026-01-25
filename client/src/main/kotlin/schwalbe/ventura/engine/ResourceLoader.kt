
package schwalbe.ventura.engine

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Resource<T>(private val loadChain: () -> () -> T) {
    
    companion object Companion
    
    
    private var loadFully: (() -> T)? = null
    var loaded: T? = null
        private set
    
    val isLoaded: Boolean
        get() = this.loaded != null
        
    operator fun invoke(): T
        = this.loaded ?: throw IllegalStateException("Resource not yet loaded")
    
    fun loadRaw() {
        check(this.loadFully == null) { "Raw resource already loaded" }
        check(!this.isLoaded) { "Resource already loaded" }
        this.loadFully = this.loadChain()
    }
    
    fun load() {
        check(!this.isLoaded) { "Resource already loaded" }
        val loadFully: () -> T = this.loadFully
            ?: throw IllegalStateException("Raw resource not yet loaded")
        this.loadFully = null
        this.loaded = loadFully()
    }
    
}

fun Resource.Companion.fromCallback(f: () -> Unit): Resource<Unit>
    = Resource { f }


class ResourceLoader {
    
    class StopException : RuntimeException()
    
    companion object {
        val stop: Resource<Unit> = Resource { throw StopException() }
    }
    
    
    private val lock = ReentrantLock()
    
    private var queued: MutableList<Resource<*>> = mutableListOf()
    private val hasQueued = this.lock.newCondition()
    
    private var loadedRaw: MutableList<Resource<*>> = mutableListOf()
    
    fun <T> submit(resource: Resource<T>): Resource<T> {
        this.lock.withLock {
            this.queued.add(resource)
            this.hasQueued.signal()
        }
        return resource
    }
    
    fun submitAll(vararg resources: Resource<*>)
        = resources.forEach { this.submit(it) }
    
    fun loadQueuedRawLoop() {
        while (true) {
            val queued: List<Resource<*>>
            this.lock.withLock {
                while (this.queued.isEmpty()) {
                    this.hasQueued.await()
                }
                queued = this.queued
                this.queued = mutableListOf()
            }
            try {
                queued.forEach(Resource<*>::loadRaw)
            } catch (_: StopException) {
                break
            }
            this.lock.withLock {
                this.loadedRaw.addAll(queued)
            }
        }
    }
    
    fun loadQueuedFully() {
        val queued: List<Resource<*>>
        this.lock.withLock {
            queued = this.loadedRaw
            this.loadedRaw = mutableListOf()
        }
        for (r in queued) {
            r.load()
        }
    }
    
}
