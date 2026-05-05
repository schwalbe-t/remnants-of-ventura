
package schwalbe.ventura.client.game

import schwalbe.ventura.client.*
import schwalbe.ventura.data.PersonColorType
import schwalbe.ventura.data.PersonHairStyle
import schwalbe.ventura.data.PersonStyle
import schwalbe.ventura.data.SharedPersonAnimation
import schwalbe.ventura.engine.AxisAlignedBox
import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.axisBoxOf
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.utils.SerVector3
import schwalbe.ventura.utils.toVector3f
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector3fc

object PersonAnim : Animations<PersonAnim> {
    val idle = anim("idle")
    val walk = anim("walk")
    val thinking = anim("thinking")
    val squat = anim("squat")
}

fun PersonAnim.fromSharedAnim(a: SharedPersonAnimation) = when (a) {
    SharedPersonAnimation.IDLE      -> PersonAnim.idle
    SharedPersonAnimation.WALK      -> PersonAnim.walk
    SharedPersonAnimation.THINKING  -> PersonAnim.thinking
    SharedPersonAnimation.SQUAT     -> PersonAnim.squat
}

fun AnimationRef<PersonAnim>.toSharedAnim() = when (this) {
    PersonAnim.idle     -> SharedPersonAnimation.IDLE
    PersonAnim.walk     -> SharedPersonAnimation.WALK
    PersonAnim.thinking -> SharedPersonAnimation.THINKING
    PersonAnim.squat    -> SharedPersonAnimation.SQUAT
    else -> SharedPersonAnimation.IDLE
}

val personModel: Resource<Model<PersonAnim>> = Model.loadFile(
    "res/person.glb",
    Renderer.meshProperties, PersonAnim,
    textureFilter = Texture.Filter.NEAREST
)

val PERSON_SHADER_MACROS: Map<String, String?> = mapOf(
    "USE_PLACEHOLDERS" to null,
    "PLACEHOLDER_COUNT" to PersonColorType.entries.size.toString(),
    "PLACEHOLDER_COLORS" to "vec3[](" + PersonColorType.entries.joinToString(
        separator = ", ",
        transform = { "vec3(${it.phR}, ${it.phG}, ${it.phB})" }
    ) + ")"
)

val personGeometryShader: Resource<Shader<GeometryVert, GeometryFrag>>
    = Shader.loadGlsl(GeometryVert, GeometryFrag, PERSON_SHADER_MACROS)

val personOutlineShader: Resource<Shader<OutlineVert, OutlineFrag>>
    = Shader.loadGlsl(OutlineVert, OutlineFrag, PERSON_SHADER_MACROS)

object Person {

    fun submitResources(loader: ResourceLoader) = loader.submitAll(
        personModel, personGeometryShader, personOutlineShader
    )

    const val MODEL_SCALE: Float = 1/5.5f
    val BASE_DIR: Vector3fc = Vector3f(0f, 0f, +1f)

    const val OUTLINE_THICKNESS: Float = 0.015f

    val relCollider: AxisAlignedBox = axisBoxOf(
        Vector3f(-0.125f, 0f, -0.125f),
        Vector3f(+0.125f, 1f, +0.125f)
    )

    fun modelTransform(
        pos: Vector3fc, rotY: Float
    ): Matrix4f = Matrix4f()
        .translate(pos)
        .rotateY(rotY)
        .scale(MODEL_SCALE)

    fun render(
        pass: RenderPass, pos: Vector3fc, rotY: Float,
        anim: AnimState<PersonAnim>, style: PersonStyle
    ) {
        val transf = modelTransform(pos, rotY)
        val instances = listOf(transf)
        val colors = style.colors.map(SerVector3::toVector3f)
        val hairMesh: String = when (style.hair) {
            PersonHairStyle.LONG -> "hair_long"
            PersonHairStyle.SHORT -> "hair_short"
        }
        val outlineShader = personOutlineShader()
        outlineShader[OutlineVert.outlineThickness] = OUTLINE_THICKNESS
        outlineShader[OutlineFrag.placeholderColors] = colors
        pass.render(
            personModel(), outlineShader,
            OutlineVert.renderer, OutlineFrag.renderer,
            anim, instances, FaceCulling.FRONT,
            renderedMeshes = listOf("body", hairMesh)
        )
        val geometryShader = personGeometryShader()
        geometryShader[GeometryFrag.placeholderColors] = colors
        pass.render(
            personModel(), geometryShader,
            GeometryVert.renderer, GeometryFrag.renderer,
            anim, instances,
            renderedMeshes = listOf("body", "eyebrows", "skull", hairMesh)
        )
    }

}
