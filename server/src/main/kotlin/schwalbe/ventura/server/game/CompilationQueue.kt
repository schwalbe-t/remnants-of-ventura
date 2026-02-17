
package schwalbe.ventura.server.game

import schwalbe.ventura.bigton.*
import schwalbe.ventura.server.QueuedWorker

class CompilationTask(
    val sources: List<BigtonSourceFile>,
    val features: Set<BigtonFeature>,
    val modules: List<BigtonModule<World>>,
    val builtinFunctions: BigtonBuiltinFunctions<World>
) {
    sealed interface Status
    object Waiting : Status
    object InProgress : Status
    class Success(val binary: ByteArray) : Status
    class Failed(val error: BigtonException) : Status

    var status: Status = Waiting
        @Synchronized get
        @Synchronized set
}

class CompilationQueue : QueuedWorker<CompilationTask>() {

    override fun completeTasks(tasks: List<CompilationTask>) {
        for (task in tasks) {
            task.status = CompilationTask.InProgress
            try {
                val programBytes: ByteArray = compileSources(
                    task.sources, task.features, task.modules,
                    task.builtinFunctions
                )
                task.status = CompilationTask.Success(programBytes)
            } catch (e: BigtonException) {
                task.status = CompilationTask.Failed(e)
            }
        }
    }

}
