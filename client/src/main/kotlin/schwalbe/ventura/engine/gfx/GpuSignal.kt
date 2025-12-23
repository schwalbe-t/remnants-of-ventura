
package schwalbe.ventura.engine.gfx

import schwalbe.ventura.engine.Disposable

import org.lwjgl.opengl.GL33.*

class GpuSignal : Disposable {

    companion object {
        fun afterPrevCommands(): GpuSignal {
            val id: Long = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0)
            glFlush()
            return GpuSignal(AwaitingFence(id))
        }
        
        val NEVER = GpuSignal(NeverReceiving)
        val RECEIVED = GpuSignal(AlreadyReceived)
    }

    
    private sealed class State
    private class AwaitingFence(val fenceId: Long) : State()
    private object NeverReceiving : State()
    private object AlreadyReceived : State()
    
    private var state: State
    
    private constructor(state: State) {
        this.state = state
    }

    fun poll(): Boolean {
        val state: State = this.state
        val fenceId: Long
        when (state) {
            is AwaitingFence    -> fenceId = state.fenceId
            is NeverReceiving   -> return false
            is AlreadyReceived  -> return true
        }
        val signaled: Boolean = when (glClientWaitSync(fenceId, 0, 0)) {
            GL_CONDITION_SATISFIED -> true
            GL_ALREADY_SIGNALED -> true
            else -> false
        }
        if (!signaled) { return false }
        glDeleteSync(state.fenceId)
        this.state = AlreadyReceived
        return true
    }
    
    override fun dispose() {
        val state: State = this.state
        if (state !is AwaitingFence) { return }
        glDeleteSync(state.fenceId)
        this.state = NeverReceiving
    }

}
