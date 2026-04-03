
package schwalbe.ventura.server.game
import schwalbe.ventura.data.*
import schwalbe.ventura.utils.SerVector3
import org.joml.Matrix4f
import org.joml.Matrix4fc

private fun SerializedWorld.triggerables(): Sequence<ObjectInstance> {
    val world: SerializedWorld = this
    return sequence {
        for (chunk in world.chunks.values) {
            for (obj in chunk.instances) {
                if (ObjectProp.Triggerable in obj) {
                    yield(obj)
                }
            }
        }
    }
}

private fun collectObjects(
    world: SerializedWorld
): Map<String, Triggerables.ObjectState> {
    val triggerables: List<ObjectInstance> = world.triggerables().toList()
    return triggerables.associateBy(
        keySelector = { obj -> obj[ObjectProp.Triggerable] },
        valueTransform = { obj ->
            val key: String = obj[ObjectProp.Triggerable]
            val inputs: Set<String> = triggerables.asSequence()
                .filter { key in it[ObjectProp.TriggerFor] }
                .mapNotNull { it.getOrNull(ObjectProp.Triggerable) }
                .toSet()
            val triggerFor: Array<String> = obj[ObjectProp.TriggerFor]
            Triggerables.ObjectState(obj, inputs, triggerFor)
        }
    )
}

class Triggerables(
    world: SerializedWorld
) {

    class ObjectState(
        val instance: ObjectInstance,
        val inputs: Set<String>,
        val triggerFor: Array<String>
    ) {
        var isTriggered: Boolean = false
            set(value) {
                if (field != value) { this.outputHasChanged = true }
                field = value
            }
        var outputHasChanged: Boolean = false
    }

    class Behavior(
        val updateBase: ObjectState.(World) -> Unit
            = { _ -> },
        val updateChained: ObjectState.(World, Triggerables) -> Unit
            = { _, _ -> }
    )

    companion object {
        const val MAX_CHAINED_ITERATIONS: Int = 1024

        val BEHAVIORS: Map<ObjectType, Behavior> = mapOf(
            ObjectType.BUTTON to BUTTON_BEHAVIOR,
            ObjectType.AND_GATE to AND_GATE_BEHAVIOR,
            ObjectType.OR_GATE to OR_GATE_BEHAVIOR,
            ObjectType.NAND_GATE to NAND_GATE_BEHAVIOR,
            ObjectType.NOR_GATE to NOR_GATE_BEHAVIOR
        )
    }


    val objects: Map<String, ObjectState> = collectObjects(world)
    private var showedIterationWarning: Boolean = false

    fun update(world: World) {
        this.objects.values.forEach {
            it.outputHasChanged = false
        }
        this.objects.values.forEach {
            BEHAVIORS[it.instance[ObjectProp.Type]]?.updateBase(it, world)
        }
        for (i in 1..MAX_CHAINED_ITERATIONS) {
            val changedObjects: List<ObjectState>
                = this.objects.values.filter { it.outputHasChanged }
            if (changedObjects.isEmpty()) { return }
            changedObjects.forEach { changed ->
                changed.triggerFor.forEach { invalidatedId ->
                    val obj = this.objects[invalidatedId] ?: return@forEach
                    BEHAVIORS[obj.instance[ObjectProp.Type]]
                        ?.updateChained(obj, world, this)
                }
                changed.outputHasChanged = false
            }
        }
        if (this.showedIterationWarning) { return }
        System.err.println(
            "WARNING! Triggerables in world '${world.name}' have exceeded " +
            "the maximum number of chained updates per tick! " +
            "(Is there a cycle?)"
        )
        this.showedIterationWarning = true
    }

    fun isTriggered(name: String): Boolean
        = this.objects[name]?.isTriggered ?: false

}

private fun baseBehavior(
    f: Triggerables.ObjectState.(World) -> Boolean
) = Triggerables.Behavior(updateBase = { world ->
    this.isTriggered = f(world)
})

private fun chainedBehavior(
    f: Triggerables.ObjectState.(World, Map<String, Boolean>) -> Boolean
) = Triggerables.Behavior(updateChained = { world, triggerables ->
    val inputs: Map<String, Boolean> = this.inputs.associateBy(
        keySelector = { it },
        valueTransform = { triggerables.objects[it]?.isTriggered ?: false }
    )
    this.isTriggered = f(world, inputs)
})

private val BUTTON_BEHAVIOR = baseBehavior { world ->
    val buttonRadius = 1f
    val buttonPos: SerVector3 = this.instance[ObjectProp.Position]
    fun SerVector3.isOnButton(): Boolean =
        buttonPos.x - buttonRadius <= this.x &&
        this.x <= buttonPos.x + buttonRadius &&
        buttonPos.z - buttonRadius <= this.z &&
        this.z <= buttonPos.z + buttonRadius
    world.players.values.any { pl ->
        val byPlayer = pl.data.worlds.last().state
            .position.isOnButton()
        val byRobot = pl.data
            .deployedRobots.values.any { it.position.isOnButton() }
        byPlayer || byRobot
    }
}

private val AND_GATE_BEHAVIOR
    = chainedBehavior { _, inputs -> inputs.values.all { it } }

private val OR_GATE_BEHAVIOR
    = chainedBehavior { _, inputs -> inputs.values.any { it } }

private val NAND_GATE_BEHAVIOR
    = chainedBehavior { _, inputs -> inputs.values.any { !it } }

private val NOR_GATE_BEHAVIOR
    = chainedBehavior { _, inputs -> inputs.values.none { it } }
