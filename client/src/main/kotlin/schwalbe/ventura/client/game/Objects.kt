
package schwalbe.ventura.client.game

import schwalbe.ventura.client.*
import schwalbe.ventura.data.ObjectInstance
import schwalbe.ventura.client.game.ChunkLoader.Companion.objectModels
import schwalbe.ventura.data.ObjectProp
import schwalbe.ventura.data.ObjectType
import schwalbe.ventura.data.buildTransform
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.Resource
import schwalbe.ventura.utils.parseRgbHex
import schwalbe.ventura.data.PersonStyle
import schwalbe.ventura.net.WorldStatePacket
import schwalbe.ventura.utils.toVector3f
import org.joml.Matrix4fc
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector3fc
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

interface ObjectStateProvider {
    fun isTriggered(obj: ObjectInstance): Boolean
    fun lastWorldState(): WorldStatePacket?
}

abstract class ObjectOverrides {
    open fun submitResources(loader: ResourceLoader) {}

    open fun update(state: ObjectStateProvider, obj: ObjectInstance) {}

    open fun transform(
        state: ObjectStateProvider, obj: ObjectInstance
    ): Matrix4f
        = obj.buildTransform()

    sealed interface RenderMethod {
        companion object {
            val DEFAULT: RenderMethod = Batched { p, _, type, transf ->
                val model: Model<StaticAnim> = objectModels
                    .getOrNull(type.ordinal)?.invoke() ?: return@Batched
                Objects.renderBatch(p, type.renderOutline, model, transf)
            }
        }

        class Single(val f: (
            pass: RenderPass, state: ObjectStateProvider,
            obj: ObjectInstance, transform: Matrix4fc
        ) -> Unit) : RenderMethod

        class Batched(val f: (
            pass: RenderPass, state: ObjectStateProvider,
            objType: ObjectType, transforms: Iterable<Matrix4fc>
        ) -> Unit) : RenderMethod
    }

    open val render: RenderMethod = RenderMethod.DEFAULT
}

object Objects {

    const val OUTLINE_THICKNESS: Float = 0.015f

    fun submitResources(resLoader: ResourceLoader) {
        this.OVERRIDES.values.forEach { it.submitResources(resLoader) }
    }

    fun update(state: ObjectStateProvider, obj: ObjectInstance) {
        val overrides = this.OVERRIDES[obj[ObjectProp.Type]] ?: return
        overrides.update(state, obj)
    }

    fun transform(state: ObjectStateProvider, obj: ObjectInstance): Matrix4f? {
        val overrides = this.OVERRIDES[obj[ObjectProp.Type]] ?: return null
        return overrides.transform(state, obj)
    }

    fun renderBatch(
        pass: RenderPass, renderOutline: Boolean,
        model: Model<StaticAnim>, instances: Iterable<Matrix4fc>,
        faceCulling: FaceCulling = FaceCulling.DISABLED,
        depthTesting: DepthTesting = DepthTesting.ENABLED,
        renderedMeshes: Collection<String>? = null,
        meshTextureOverrides: Map<String, Texture>? = null,
        shadowFactorOverride: Vector3fc? = null,
        outlineFactorOverride: Vector3fc? = null
    ) {
        if (renderOutline) {
            pass.renderOutline(
                model, this.OUTLINE_THICKNESS, animState = null, instances,
                depthTesting, renderedMeshes, meshTextureOverrides,
                outlineFactorOverride
            )
        }
        pass.renderGeometry(
            model, animState = null, instances,
            faceCulling, depthTesting, renderedMeshes, meshTextureOverrides,
            shadowFactorOverride
        )
    }

    fun renderMethodOf(objType: ObjectType): ObjectOverrides.RenderMethod
        = OVERRIDES[objType]?.render ?: ObjectOverrides.RenderMethod.DEFAULT

    fun renderMethodOf(obj: ObjectInstance): ObjectOverrides.RenderMethod
        = this.renderMethodOf(obj[ObjectProp.Type])

    val OVERRIDES: Map<ObjectType, ObjectOverrides> = mapOf(
        ObjectType.TREE_GREEN to FoliageOverrides(
            TREE_FOLIAGE,
            colorBottom = parseRgbHex("437f5d"),
            colorTop    = parseRgbHex("86a063")
        ),
        ObjectType.TREE_ORANGE to FoliageOverrides(
            TREE_FOLIAGE,
            colorBottom = parseRgbHex("cc785b"),
            colorTop    = parseRgbHex("d3925b")
        ),
        ObjectType.TREE_RED to FoliageOverrides(
            TREE_FOLIAGE,
            colorBottom = parseRgbHex("94554d"),
            colorTop    = parseRgbHex("cc785b")
        ),
        ObjectType.TREE_PINK to FoliageOverrides(
            TREE_FOLIAGE,
            colorBottom = parseRgbHex("aa749e"),
            colorTop    = parseRgbHex("d4a488")
        ),
        ObjectType.LUSH_GRASS to FoliageOverrides(
            GRASS_FOLIAGE,
            colorBottom = parseRgbHex("86a063"),
            colorTop    = parseRgbHex("437f5d")
        ),
        ObjectType.DRY_GRASS to FoliageOverrides(
            GRASS_FOLIAGE,
            colorBottom = parseRgbHex("d4a488"),
            colorTop    = parseRgbHex("705448")
        ),
        ObjectType.LUSH_BUSH to FoliageOverrides(
            BUSH_FOLIAGE,
            colorBottom = parseRgbHex("437f5d"),
            colorTop    = parseRgbHex("86a063")
        ),
        ObjectType.DRY_BUSH to FoliageOverrides(
            BUSH_FOLIAGE,
            colorBottom = parseRgbHex("705448"),
            colorTop    = parseRgbHex("d4a488")
        ),
        ObjectType.TUMBLEWEED to TumbleweedOverrides,
        ObjectType.BUTTON to ButtonOverrides,
        ObjectType.LAMP to LampOverrides,
        ObjectType.CHARACTER to CharacterOverrides
    )

}

private val TREE_FOLIAGE = FoliageOverrides.FoliageType(
    model = Model.loadFile(
        path = "res/objects/tree.glb",
        properties = Renderer.meshProperties,
        textureFilter = Texture.Filter.NEAREST
    ),
    foliageMeshes = listOf("leaves"), staticMeshes = listOf("trunk"),
    swayInterval = 5f, swayAmount = 0.1f,
    bottom = +1.5f, top = +6f
)

private val GRASS_FOLIAGE = FoliageOverrides.FoliageType(
    model = Model.loadFile(
        path = "res/objects/grass.glb",
        properties = Renderer.meshProperties,
        textureFilter = Texture.Filter.NEAREST
    ),
    foliageMeshes = listOf("grass"), staticMeshes = listOf(),
    diffuseThreshold = -1f,
    swayInterval = 5f, swayAmount = 0.1f,
    bottom = 0f, top = +1f
)

private val BUSH_FOLIAGE = FoliageOverrides.FoliageType(
    model = Model.loadFile(
        path = "res/objects/bush.glb",
        properties = Renderer.meshProperties,
        textureFilter = Texture.Filter.NEAREST
    ),
    foliageMeshes = listOf("bush"), staticMeshes = listOf(),
    swayInterval = 5f, swayAmount = 0.1f,
    bottom = 0f, top = +1f
)

private class FoliageOverrides(
    val type: FoliageType, val colorBottom: Vector3fc, val colorTop: Vector3fc
) : ObjectOverrides() {

    class FoliageType(
        val model: Resource<Model<StaticAnim>>,
        val swayInterval: Float, val swayAmount: Float,
        val bottom: Float, val top: Float,
        val foliageMeshes: List<String>, val staticMeshes: List<String>,
        val diffuseThreshold: Float = 0f
    )

    companion object {
        private val perlinMap: Resource<Texture>
            = Texture.loadImage("res/vfx/perlin.png", Texture.Filter.LINEAR)

        private object FoliageVert : VertShaderDef<FoliageVert> {
            override val path: String = "shaders/foliage.vert.glsl"

            val renderer = RendererVert<FoliageVert>()
            val bottomHeight = float("uBottomHeight")
            val topHeight = float("uTopHeight")
            val swayInterval = float("uSwayInterval")
            val swayAmount = float("uSwayAmount")
            val perlinMap = sampler2D("uPerlinMap")
        }

        private object FoliageFrag : FragShaderDef<FoliageFrag> {
            override val path: String = "shaders/foliage.frag.glsl"

            val renderer = RendererFrag<FoliageFrag>()
            val bottomHeight = float("uBottomHeight")
            val bottomColor = vec3("uBottomColor")
            val topHeight = float("uTopHeight")
            val topColor = vec3("uTopColor")
        }
    }

    private val foliageShader: Resource<Shader<FoliageVert, FoliageFrag>>
        = Shader.loadGlsl(FoliageVert, FoliageFrag, macros = mapOf(
            "DIFFUSE_THRESHOLD" to "${this.type.diffuseThreshold}"
        ))

    override fun submitResources(loader: ResourceLoader)
        = loader.submitAll(this.type.model, perlinMap, this.foliageShader)

    override val render = RenderMethod.Batched { pass, _, _, transforms ->
        val model: Model<StaticAnim> = this.type.model()
        val shader: Shader<FoliageVert, FoliageFrag> = this.foliageShader()
        shader[FoliageVert.bottomHeight] = this.type.bottom
        shader[FoliageVert.topHeight] = this.type.top
        shader[FoliageVert.swayInterval] = this.type.swayInterval
        shader[FoliageVert.swayAmount] = this.type.swayAmount
        shader[FoliageVert.perlinMap] = perlinMap()
        // shader[FoliageFrag.bottomHeight] = this.bottom
        shader[FoliageFrag.bottomColor] = this.colorBottom
        // shader[FoliageFrag.topHeight] = this.top
        shader[FoliageFrag.topColor] = this.colorTop
        pass.render(
            model, shader, FoliageVert.renderer, FoliageFrag.renderer,
            animState = null, transforms,
            renderedMeshes = this.type.foliageMeshes
        )
        Objects.renderBatch(
            pass, renderOutline = true, model, transforms,
            renderedMeshes = this.type.staticMeshes
        )
    }

}

private object TumbleweedOverrides : ObjectOverrides() {

    private val model: Resource<Model<StaticAnim>> = Model.loadFile(
        path = "res/objects/tumbleweed.glb",
        properties = Renderer.meshProperties,
        textureFilter = Texture.Filter.NEAREST
    )

    override fun submitResources(loader: ResourceLoader)
        = loader.submitAll(this.model)

    const val SELF_SPEED: Float = 1f // radians/s
    const val BASE_HEIGHT: Float = 0.25f // u
    const val BOUNCE_HEIGHT: Float = 0.25f // u
    const val ORBIT_SPEED: Float = 0.05f // radians/s
    const val RADIUS: Float = 10f // u

    private fun transform(transform: Matrix4fc): Matrix4f {
        val time: Double = System.currentTimeMillis() / 1000.0
        val selfAngle: Float = (time * SELF_SPEED).mod(2 * PI).toFloat()
        val orbAngle: Float = (time * ORBIT_SPEED).mod(2 * PI).toFloat()
        val height: Float = BASE_HEIGHT + abs(sin(selfAngle)) * BOUNCE_HEIGHT
        return Matrix4f(transform)
            .rotateY(orbAngle)
            .translate(RADIUS, height, 0f)
            .rotateX(-selfAngle)
    }

    override val render = RenderMethod.Batched { pass, _, _, transforms ->
        Objects.renderBatch(
            pass, renderOutline = false, this.model(),
            transforms.map(this::transform)
        )
    }

}

private object ButtonOverrides : ObjectOverrides() {

    const val HEAD_TRIGGER_OFFSET: Float = -0.08f

    override val render = RenderMethod.Single { pass, state, obj, transform ->
        val model: Model<StaticAnim> = objectModels
            .getOrNull(ObjectType.BUTTON.ordinal)?.invoke() ?: return@Single
        val isDown: Boolean = state.isTriggered(obj)
        val headTransf: Matrix4fc = Matrix4f(transform)
            .translateLocal(0f, if (isDown) HEAD_TRIGGER_OFFSET else 0f, 0f)
        Objects.renderBatch(
            pass, renderOutline = true, model, instances = listOf(transform),
            renderedMeshes = listOf("button_base")
        )
        Objects.renderBatch(
            pass, renderOutline = true, model, instances = listOf(headTransf),
            renderedMeshes = listOf("button_head")
        )
    }

}

private object LampOverrides : ObjectOverrides() {

    val lampOffTexture: Resource<Texture>
        = Texture.loadImage("res/objects/lamp_off.png", Texture.Filter.NEAREST)
    val lampOnTexture: Resource<Texture>
        = Texture.loadImage("res/objects/lamp_on.png", Texture.Filter.NEAREST)

    override fun submitResources(loader: ResourceLoader)
        = loader.submitAll(this.lampOffTexture, this.lampOnTexture)

    override val render = RenderMethod.Single { pass, state, obj, transform ->
        val model: Model<StaticAnim> = objectModels
            .getOrNull(ObjectType.LAMP.ordinal)?.invoke() ?: return@Single
        val instances = listOf(transform)
        val isOn: Boolean = state.isTriggered(obj)
        val tex: Texture
                = if (isOn) this.lampOnTexture() else this.lampOffTexture()
        val texOverrides: Map<String, Texture> = mapOf(
            "lamp_base" to tex, "lamp_filament" to tex, "lamp_glass" to tex
        )
        val litShadowFactor = if (isOn) Vector3f(1f, 1f, 1f) else null
        Objects.renderBatch(
            pass, renderOutline = true, model, instances,
            renderedMeshes = listOf("lamp_base"),
            meshTextureOverrides = texOverrides
        )
        Objects.renderBatch(
            pass, renderOutline = true, model, instances,
            renderedMeshes = listOf("lamp_filament"),
            meshTextureOverrides = texOverrides,
            shadowFactorOverride = litShadowFactor
        )
        pass.renderGeometry(
            model, animState = null, instances,
            faceCulling = FaceCulling.BACK,
            renderedMeshes = listOf("lamp_glass"),
            meshTextureOverrides = texOverrides,
            shadowFactorOverride = litShadowFactor
        )
    }

}

private object CharacterOverrides : ObjectOverrides() {

    private fun getStyle(obj: ObjectInstance): PersonStyle
        = if (ObjectProp.CharacterStylePreset in obj) {
            obj[ObjectProp.CharacterStylePreset].style
        } else {
            obj[ObjectProp.CharacterStyleCustom].toPersonStyle()
        }

    private fun closestPlayer(
        state: ObjectStateProvider, to: Vector3fc
    ): Vector3f? = state.lastWorldState()
        ?.players?.values?.minByOrNull { it.position.toVector3f().distance(to) }
        ?.position?.toVector3f()

    const val NEAR_LOOK_DIST: Float = 3f // dist <= this -> weight = 1
    const val FAR_LOOK_DIST: Float = 5f // dist >= this -> weight = 0
    const val LOOK_DIST_RANGE: Float = FAR_LOOK_DIST - NEAR_LOOK_DIST

    override val render = RenderMethod.Single { pass, state, obj, _ ->
        val pos = obj[ObjectProp.Position].toVector3f()
        val rotY = obj[ObjectProp.Rotation].y
        val anim = PersonAnim.fromSharedAnim(obj[ObjectProp.CharacterAnimation])
        val animLen: Double = personModel().animations[anim.name]
            ?.lengthSecs?.toDouble() ?: 1.0
        val animTime = (System.currentTimeMillis().toDouble() / 1000) % animLen
        val animState = AnimState(anim)
        animState.addAnimationTimePassed(animTime.toFloat())
        closestPlayer(state, pos)?.let { target ->
            val dist: Float = pos.distance(target)
            target.add(0f, 1.5f, 0f)
            val weight: Float = ((FAR_LOOK_DIST - dist) / LOOK_DIST_RANGE)
                .coerceIn(0f, 1f)
            Person.facePoint(pos, rotY, target, weight, animState)
        }
        Person.render(pass, pos, rotY, animState, getStyle(obj))
    }

}
