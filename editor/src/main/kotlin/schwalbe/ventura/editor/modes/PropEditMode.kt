
package schwalbe.ventura.editor.modes

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.joml.Vector4f
import schwalbe.ventura.data.ObjectInstance
import schwalbe.ventura.data.ObjectProp
import schwalbe.ventura.editor.*
import schwalbe.ventura.engine.ui.*
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.jvm.jvmName

fun <T : Any> KClass<T>.allConcreteSealedSubclasses(): List<KClass<out T>> {
    return this.sealedSubclasses.flatMap {
        if (!it.isSealed) listOf(it)
        else it.allConcreteSealedSubclasses()
    }
}

private class ObjectPropEditor(
    val propTypeClass: KClass<out ObjectProp<*>>,
    val propType: ObjectProp.PropType<*, *>,
    instance: ObjectInstance,
    val world: LoadedWorld,
    val editor: Editor
) {

    companion object {
        val LINE_HEIGHT: UiSize = 3.5.vmin
        val HEIGHT: UiSize = 2 * LINE_HEIGHT
        val TOGGLE_BUTTON_WIDTH: UiSize = 10.vmin

        val SERIALIZER = Json {}
    }

    var enabled: Boolean = instance.props.any { propTypeClass.isInstance(it) }

    val titleText = Text()
        .withText(propTypeClass.simpleName ?: propTypeClass.jvmName)
        .withSize(70.ph)
    val toggleButtonText = Text()
        .withSize(70.ph)
        .alignCenter()
    val toggleButton = Stack()
        .add(FlatBackground()
            .withColor(TRANSPARENT)
            .withHoverColor(HOVER_COLOR)
        )
        .add(this.toggleButtonText
            .pad(0.75.vmin)
        )
        .add(ClickArea().withLeftHandler {
            this.enabled = !this.enabled
            when (this.enabled) {
                true -> this.addProp()
                false -> this.removeProp()
            }
            this.resetValueInput()
        })

    val valueInputText = Text()
        .withSize(70.ph)
    val valueInput = TextInput()
        .withContent(this.valueInputText)
        .let { inp -> inp
            .withTypedCodepoints {
                if (this.enabled) { inp.writeText(it) }
            }
        }
        .withValueChangedHandler {
            this.writeEnteredPropValue()
        }

    val root: UiElement = Axis.column(LINE_HEIGHT)
        .add(Axis.row()
            .add(100.pw - TOGGLE_BUTTON_WIDTH, this.titleText
                .pad(0.75.vmin)
            )
            .add(TOGGLE_BUTTON_WIDTH, this.toggleButton)
        )
        .add(Stack()
            .add(FlatBackground()
                .withColor(TRANSPARENT)
                .withHoverColor(HOVER_COLOR)
            )
            .add(this.valueInput
                .pad(0.5.vmin)
            )
        )

    init {
        this.resetValueInput()
    }

    private fun resetValueInput() {
        val value: Any? = this.editor.getSelectedObject()
            ?.props?.firstOrNull { this.propTypeClass.isInstance(it) }
            ?.v
        if (!this.enabled || value == null) {
            this.valueInput.withValue("")
        } else {
            val serializer = SERIALIZER.serializersModule
                .serializer(value::class.java)
            val valueStr = SERIALIZER.encodeToString(serializer, value)
            this.valueInput.withValue(valueStr)
        }
    }

    private fun writeEnteredPropValue() {
        val serializer = SERIALIZER.serializersModule
            .serializer(this.propType.default::class.java)
        val prop: ObjectProp<*>
        try {
            val value: Any = SERIALIZER.decodeFromString(
                serializer, this.valueInput.valueString
            )
            prop = this.propTypeClass.constructors
                .first { it.parameters.size == 1 }
                .call(value)
        } catch (e: Exception) {
            this.valueInputText.withColor(255, 0, 0)
            return
        }
        this.modifyProps { props ->
            props.filter { !this.propTypeClass.isInstance(it) } + listOf(prop)
        }
        this.valueInputText.withColor(null)
    }

    private fun modifyProps(f: (List<ObjectProp<*>>) -> List<ObjectProp<*>>) {
        val oldRef: ObjectInstanceRef = this.world.selectedObject ?: return
        this.world.selectedObject = this.world.withObjectEdit(oldRef) { inst ->
            ObjectInstance(f(inst.props))
        }
    }

    private fun removeProp() {
        this.modifyProps { props -> props
            .filter { !this.propTypeClass.isInstance(it) }
        }
    }

    private fun addProp() {
        val defaultValue: Any? = this.propType.default
        val propInstance: ObjectProp<*> = this.propTypeClass.constructors
            .first { it.parameters.size == 1 }
            .call(defaultValue)
        this.modifyProps { props -> props + listOf(propInstance) }
    }

    fun update() {
        this.toggleButtonText.withText(
            if (this.enabled) "Enabled" else "Disabled"
        )
        this.titleText.withColor(
            if (this.enabled) null else Vector4f(0.5f, 0.5f, 0.5f, 1f)
        )
    }

}

fun propEditMode(editor: Editor): () -> EditorMode = {
    val propList: SizedAxis = Axis.column(ObjectPropEditor.HEIGHT)
    val propEditors = mutableListOf<ObjectPropEditor>()
    val world: LoadedWorld? = editor.world
    val selected: ObjectInstance? = editor.getSelectedObject()
    if (world != null && selected != null) {
        val propTypeClasses = ObjectProp::class.allConcreteSealedSubclasses()
            .sortedBy { it.simpleName ?: it.jvmName }
        for (propTypeClass in propTypeClasses) {
            val propType = propTypeClass.companionObjectInstance
                as? ObjectProp.PropType<*, *> ?: continue
            val editor = ObjectPropEditor(
                propTypeClass, propType, selected, world, editor
            )
            propEditors.add(editor)
            propList.add(editor.root)
        }
    }
    propList.add(50.ph, Space())
    fun updatePropEditors() {
        for (editor in propEditors) {
            editor.update()
        }
    }
    val mode = EditorMode(
        render = {
            editor.update()
            updatePropEditors()
            editor.render()
        },
        navigator = editor.nav
    )
    mode.add(layer = 0, element = Axis.row(fpw / 3f)
        .add(Stack()
            .add(FlatBackground().withColor(BACKGROUND_COLOR))
            .add(propList
                .wrapScrolling(horiz = false, vert = true)
            )
        )
        .add(Space())
        .add(createModeDisplay("Property Edit Mode", Space()))
    )
    mode
}
