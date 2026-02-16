
package schwalbe.ventura.server

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class QueuedWorker<T> {

    private val lock = ReentrantLock()
    private var tasks = mutableListOf<T>()
    private val hasTask = this.lock.newCondition()

    fun add(task: T) {
        this.lock.withLock {
            this.tasks.add(task)
            this.hasTask.signal()
        }
    }

    abstract fun completeTasks(tasks: List<T>)

    fun runWorker() {
        while (true) {
            val completing: List<T>
            this.lock.withLock {
                while (this.tasks.isEmpty()) {
                    this.hasTask.await()
                }
                completing = this.tasks
                this.tasks = mutableListOf<T>()
            }
            this.completeTasks(completing)
        }
    }

}