
package schwalbe.ventura.bigton.runtime

/**
 * Mirror of 'bigton_exec_status_t'
 * defined in 'src/main/headers/bigton/runtime.h'
 */
object BigtonExecStatusType {
    const val CONTINUE = 0
    const val EXEC_BUILTIN_FUN = 1
    const val AWAIT_TICK = 2
    const val COMPLETE = 3
    const val ERROR = 4
}

sealed class BigtonExecStatus {
    
    object Continue
        : BigtonExecStatus()

    class ExecBuiltinFun(val id: Int)
        : BigtonExecStatus()

    object AwaitTick
        : BigtonExecStatus()

    object Complete
        : BigtonExecStatus()

    class Error(val error: BigtonRuntimeError)
        : BigtonExecStatus()

}
