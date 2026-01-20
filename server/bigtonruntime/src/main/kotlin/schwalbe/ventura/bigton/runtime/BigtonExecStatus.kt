
package schwalbe.ventura.bigton.runtime

/**
 * Mirror of 'bigton_exec_status_t'
 * defined in 'src/main/headers/bigton/runtime.h'
 */
enum class BigtonExecStatus {
    CONTINUE,
    EXEC_BUILTIN_FUN,
    AWAIT_TICK,
    COMPLETE,
    ERROR
}