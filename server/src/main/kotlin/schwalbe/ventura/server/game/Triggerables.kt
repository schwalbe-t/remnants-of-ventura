
package schwalbe.ventura.server.game

import schwalbe.ventura.data.*
import schwalbe.ventura.net.SerVector3
import org.joml.Vector3fc
import schwalbe.ventura.net.toVector3f

private fun SerializedWorld.triggerables(): Sequence<ObjectInstance> {
    val world: SerializedWorld = this
    return sequence {
        for (chunk in world.chunks.values) {
            for (obj in chunk.instances) {
                if (obj.props.any { it is ObjectProp.Triggerable }) {
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
            val inputs: Set<String> = triggerables.asSequence()
                .mapNotNull { it.props
                    .filterIsInstance<ObjectProp.Triggerable>()
                    .firstOrNull()?.v
                }
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

    companion object {
        const val MAX_CHAINED_ITERATIONS: Int = 1024
    }


    val objects: Map<String, ObjectState> = collectObjects(world)
    private var showedIterationWarning: Boolean = false

    fun update(world: World) {
        this.objects.values.forEach {
            it.outputHasChanged = false
        }
        this.objects.values.forEach {
            it.updateBase(world)
        }
        for (i in 1..MAX_CHAINED_ITERATIONS) {
            val changedObjects: List<ObjectState>
                = this.objects.values.filter { it.outputHasChanged }
            if (changedObjects.isEmpty()) { return }
            changedObjects.forEach { changed ->
                changed.triggerFor.forEach { invalidatedId ->
                    this.objects[invalidatedId]?.updateChained(world, this)
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

}

private fun Triggerables.ObjectState.updateBase(world: World) {
    when (this.instance[ObjectProp.Type]) {
        ObjectType.BUTTON -> {
            val buttonRadius = 1f
            val buttonPos: SerVector3 = this.instance[ObjectProp.Position]
            fun SerVector3.isOnButton(): Boolean =
                buttonPos.x - buttonRadius <= this.x &&
                this.x <= buttonPos.x + buttonRadius &&
                buttonPos.z - buttonRadius <= this.z &&
                this.z <= buttonPos.z + buttonRadius
            this.isTriggered = world.players.values.any { pl ->
                val byPlayer = pl.data.worlds.last().state
                    .position.isOnButton()
                val byRobot = pl.data
                    .deployedRobots.values.any { it.position.isOnButton() }
                byPlayer || byRobot
            }
        }
        else -> {}
    }
}

private fun Triggerables.ObjectState.updateChained(
    world: World, triggerables: Triggerables
) {
    val inputValues: Map<String, Boolean> = this.inputs.associateBy(
        keySelector = { it },
        valueTransform = { triggerables.objects[it]?.isTriggered ?: false }
    )
    when (this.instance[ObjectProp.Type]) {
        ObjectType.AND_GATE -> {
            this.isTriggered = inputValues.values.all { it }
        }
        ObjectType.OR_GATE -> {
            this.isTriggered = inputValues.values.any { it }
        }
        ObjectType.NAND_GATE -> {
            this.isTriggered = inputValues.values.any { !it }
        }
        ObjectType.NOR_GATE -> {
            this.isTriggered = inputValues.values.none { it }
        }
        // TODO! Add door implementation here
        else -> {}
    }
}
