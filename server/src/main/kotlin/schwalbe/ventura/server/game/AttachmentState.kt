
package schwalbe.ventura.server.game

import kotlinx.serialization.Serializable

@Serializable
class AttachmentStates<S> {

    @Serializable
    data class StateType<V>(
        val id: Int,
        val default: () -> V
    )

    companion object {
        private var nextId: Int = 0

        fun <V> register(default: () -> V): StateType<V> {
            val id: Int = this.nextId
            this.nextId += 1
            return StateType(id, default)
        }
    }


    val states: MutableMap<Int, S> = mutableMapOf()

    inline operator fun <reified V : S> get(type: StateType<V>): V {
        val existing: V? = this.states[type.id] as? V
        if (existing != null) { return existing }
        val new: V = type.default()
        this.states[type.id] = new
        return new
    }

}
