
package schwalbe.ventura.client

import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.net.toVector3f
import schwalbe.ventura.data.RendererConfig
import kotlin.collections.toMutableList
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import org.joml.*

class RendererVert<S : VertShaderDef<S>> : VertShaderDef<S> {
    companion object {
        const val MAX_NUM_INSTANCES = 1000
        const val MAX_NUM_JOINTS = 64
    }

    override val path = "shaders/common/renderer.vert.glsl"

    val instances = block("uInstances")

    val localTransform = mat4("uLocalTransform")
    val jointTransforms = mat4Arr("uJointTransforms", MAX_NUM_JOINTS)
    val viewProjection = mat4("uViewProjection")
}

class RendererFrag<S : FragShaderDef<S>> : FragShaderDef<S> {
    override val path = "shaders/common/renderer.frag.glsl"

    val texture = sampler2D("uTexture")
    val baseFactor = vec3("uBaseFactor")
    val shadowFactor = vec3("uShadowFactor")
    val outlineFactor = vec3("uOutlineFactor")
    val groundToSun = vec3("uGroundToSun")
    val sunViewProjection = mat4("uSunViewProjection")
    val shadowMap = sampler2DMS("uShadowMap")
    val shadowMapSamples = int("uShadowMapSamples")
    val depthBias = float("uDepthBias")
    val normalOffset = float("uNormalOffset")
    val defaultLit = bool("uDefaultLit")
}


object GeometryVert : VertShaderDef<GeometryVert> {
    override val path: String = "shaders/geometry.vert.glsl"

    val renderer = RendererVert<GeometryVert>()
}

object GeometryFrag : FragShaderDef<GeometryFrag> {
    override val path: String = "shaders/geometry.frag.glsl"

    val renderer = RendererFrag<GeometryFrag>()
}

val geometryShader: Resource<Shader<GeometryVert, GeometryFrag>>
        = Shader.loadGlsl(GeometryVert, GeometryFrag)


object OutlineVert : VertShaderDef<OutlineVert> {
    override val path: String = "shaders/outline.vert.glsl"

    val renderer = RendererVert<OutlineVert>()
    val outlineThickness = float("uOutlineThickness")
}

object OutlineFrag : FragShaderDef<OutlineFrag> {
    override val path: String = "shaders/outline.frag.glsl"

    val renderer = RendererFrag<OutlineFrag>()
}

val outlineShader: Resource<Shader<OutlineVert, OutlineFrag>>
        = Shader.loadGlsl(OutlineVert, OutlineFrag)


object DepthOnlyFrag : FragShaderDef<DepthOnlyFrag> {
    override val path: String = "shaders/depth.frag.glsl"

    val renderer = RendererFrag<DepthOnlyFrag>()
}

val depthOnlyGeometryShader: Resource<Shader<GeometryVert, DepthOnlyFrag>>
        = Shader.loadGlsl(GeometryVert, DepthOnlyFrag)

object DiscardFrag : FragShaderDef<DiscardFrag> {
    override val path: String = "shaders/discard.frag.glsl"

    val renderer = RendererFrag<DiscardFrag>()
}

val discardOutlineShader: Resource<Shader<OutlineVert, DiscardFrag>>
        = Shader.loadGlsl(OutlineVert, DiscardFrag)


class Renderer(val dest: ConstFramebuffer) {

    companion object {
        val meshProperties: List<Model.Property> = listOf(
            Model.Property.POSITION,
            Model.Property.NORMAL,
            Model.Property.UV,
            Model.Property.BONE_IDS_BYTE,
            Model.Property.BONE_WEIGHTS
        )
        val geometryAttribs: List<Geometry.Attribute> = listOf(
            Geometry.float(3),
            Geometry.float(3),
            Geometry.float(2),
            Geometry.ubyte(4),
            Geometry.float(4)
        )

        fun submitResources(loader: ResourceLoader) = loader.submitAll(
            geometryShader, outlineShader,
            depthOnlyGeometryShader, discardOutlineShader
        )

        const val SUN_NEAR: Float = 1f
        const val SUN_FAR: Float = 200f
        const val SUN_DISTANCE: Float = 100f
        val SUN_UP: Vector3fc = Vector3f(0f, +1f, 0f)
        const val DEPTH_BIAS: Float = 0.00075f
        const val NORMAL_OFFSET: Float = 0.01f

        const val SHADOW_MAP_RES: Int = 2048
        const val SHADOW_MAP_SAMPLES: Int = 4
    }


    val camera = Camera()
    var sunDiameter: Float = 32f

    var config: RendererConfig = RendererConfig.default

    private var mutCamViewProj = Matrix4f()
    private var mutSunViewProj = Matrix4f()
    val camViewProj: Matrix4fc = this.mutCamViewProj
    val sunViewProj: Matrix4fc = this.mutSunViewProj

    val shadowMapTex = Texture(
        SHADOW_MAP_RES, SHADOW_MAP_RES,
        Texture.Filter.NEAREST, Texture.Format.DEPTH32,
        SHADOW_MAP_SAMPLES
    )
    private val mutShadowMap = Framebuffer()
        .attachDepth(this.shadowMapTex)
    val shadowMap: ConstFramebuffer = this.mutShadowMap

    val instances = UniformBuffer(BufferWriteFreq.EVERY_FRAME)
    val instanceBuff: ByteBuffer
            = ByteBuffer.allocateDirect(
        RendererVert.MAX_NUM_INSTANCES * 4*4 * Geometry.Type.FLOAT.numBytes
    )
        .order(ByteOrder.nativeOrder())

    fun update(sunTarget: Vector3fc) {
        this.mutCamViewProj.set(this.camera.computeViewProj(this.dest))
        val sunPos: Vector3fc = this.config.groundToSun.toVector3f()
            .mul(SUN_DISTANCE)
            .add(sunTarget)
        this.mutSunViewProj
            .setOrthoSymmetric(
                this.sunDiameter, this.sunDiameter, SUN_NEAR, SUN_FAR
            )
            .lookAt(sunPos, sunTarget, SUN_UP)
    }

    fun beginShadowPass(): RenderPass {
        this.shadowMap.clearDepth(1f)
        return TypedRenderPass(
            this,
            this.mutSunViewProj,
            depthOnlyGeometryShader, DepthOnlyFrag.renderer,
            discardOutlineShader, DiscardFrag.renderer,
            this.shadowMap
        )
    }

    fun beginGeometryPass(): RenderPass {
        this.dest.clearColor(Vector4f(0.0f, 0.0f, 0.0f, 0.0f))
        this.dest.clearDepth(1f)
        return TypedRenderPass(
            this,
            this.mutCamViewProj,
            geometryShader, GeometryFrag.renderer,
            outlineShader, OutlineFrag.renderer,
            this.dest
        )
    }

    inline fun forEachPass(crossinline f: (RenderPass) -> Unit) {
        f(this.beginShadowPass())
        f(this.beginGeometryPass())
    }

    fun dispose() {
        this.instances.dispose()
        this.mutShadowMap.dispose()
        this.shadowMapTex.dispose()
    }

}


typealias RenderPass = TypedRenderPass<*, *>

class TypedRenderPass<
        FGeometry : FragShaderDef<FGeometry>,
        FOutline : FragShaderDef<FOutline>
        >(
    val renderer: Renderer,
    val viewProj: Matrix4fc,
    val geometryShader: Resource<Shader<GeometryVert, FGeometry>>,
    val geometryFragShader: RendererFrag<FGeometry>,
    val outlineShader: Resource<Shader<OutlineVert, FOutline>>,
    val outlineFragShader: RendererFrag<FOutline>,
    val dest: ConstFramebuffer
) {

    fun <V : VertShaderDef<V>, F : FragShaderDef<F>> configureShader(
        shader: Shader<V, F>,
        vertShader: RendererVert<V>,
        fragShader: RendererFrag<F>
    ) {
        val cfg = this.renderer.config
        shader[vertShader.viewProjection] = this.viewProj
        shader[fragShader.baseFactor] = cfg.baseColorFactor.toVector3f()
        shader[fragShader.shadowFactor] = cfg.shadowColorFactor.toVector3f()
        shader[fragShader.outlineFactor] = cfg.outlineColorFactor.toVector3f()
        shader[fragShader.groundToSun] = cfg.groundToSun.toVector3f()
        shader[fragShader.sunViewProjection] = this.renderer.sunViewProj
        shader[fragShader.shadowMap] = this.renderer.shadowMapTex
        shader[fragShader.shadowMapSamples] = Renderer.SHADOW_MAP_SAMPLES
        shader[fragShader.depthBias] = Renderer.DEPTH_BIAS
        shader[fragShader.normalOffset] = Renderer.NORMAL_OFFSET
        shader[fragShader.defaultLit] = cfg.defaultLit
    }

    fun <A : Animations<A>> renderGeometry(
        model: Model<A>,
        animState: AnimState<A>? = null,
        instances: Iterable<Matrix4fc> = listOf(Matrix4f()),
        faceCulling: FaceCulling = FaceCulling.DISABLED,
        depthTesting: DepthTesting = DepthTesting.ENABLED,
        renderedMeshes: Collection<String>? = null,
        meshTextureOverrides: Map<String, Texture>? = null
    ) {
        this.render(
            model,
            this.geometryShader(),
            GeometryVert.renderer, this.geometryFragShader,
            animState, instances,
            faceCulling, depthTesting,
            renderedMeshes, meshTextureOverrides
        )
    }

    fun <A : Animations<A>> renderOutline(
        model: Model<A>,
        outlineThickness: Float = 0.1f,
        animState: AnimState<A>? = null,
        instances: Iterable<Matrix4fc> = listOf(Matrix4f()),
        depthTesting: DepthTesting = DepthTesting.ENABLED,
        renderedMeshes: Collection<String>? = null,
        meshTextureOverrides: Map<String, Texture>? = null
    ) {
        val shader = this.outlineShader()
        shader[OutlineVert.outlineThickness] = outlineThickness
        this.render(
            model,
            shader, OutlineVert.renderer, this.outlineFragShader,
            animState, instances,
            FaceCulling.FRONT, depthTesting,
            renderedMeshes, meshTextureOverrides
        )
    }

    private inline fun withInstanceBatches(
        all: Iterable<Matrix4fc>, crossinline f: (Int, UniformBuffer) -> Unit
    ) {
        val maxBatchSize: Int = RendererVert.MAX_NUM_INSTANCES
        val remaining: MutableList<Matrix4fc> = all.toMutableList()
        while (remaining.isNotEmpty()) {
            val batchSize: Int = minOf(remaining.size, maxBatchSize)
            val batch: MutableList<Matrix4fc> = remaining.subList(0, batchSize)
            this.renderer.instanceBuff.clear()
            val buff: FloatBuffer = this.renderer.instanceBuff.asFloatBuffer()
            for (i in 0..<batch.size) {
                batch[i].get(i * 16, buff)
            }
            this.renderer.instances.write(this.renderer.instanceBuff)
            f(batchSize, this.renderer.instances)
            batch.clear()
        }
    }

    fun <V : VertShaderDef<V>, F : FragShaderDef<F>, A : Animations<A>> render(
        model: Model<A>,
        shader: Shader<V, F>,
        vertShader: RendererVert<V>,
        fragShader: RendererFrag<F>,
        animState: AnimState<A>? = null,
        instances: Iterable<Matrix4fc> = listOf(Matrix4f()),
        faceCulling: FaceCulling = FaceCulling.DISABLED,
        depthTesting: DepthTesting = DepthTesting.ENABLED,
        renderedMeshes: Collection<String>? = null,
        meshTextureOverrides: Map<String, Texture>? = null
    ) {
        this.configureShader(shader, vertShader, fragShader)
        this.withInstanceBatches(instances) { batchSize, buff ->
            shader[vertShader.instances] = buff
            model.render(
                shader, this.dest,
                vertShader.localTransform,
                fragShader.texture,
                vertShader.jointTransforms,
                animState,
                instanceCount = batchSize,
                faceCulling, depthTesting,
                renderedMeshes, meshTextureOverrides
            )
        }
    }

    fun <V : VertShaderDef<V>, F : FragShaderDef<F>, A : Animations<A>> render(
        geometry: Geometry,
        shader: Shader<V, F>,
        vertShader: RendererVert<V>,
        fragShader: RendererFrag<F>,
        instances: Iterable<Matrix4fc> = listOf(Matrix4f()),
        localTransform: Matrix4fc = Matrix4f().identity(),
        texture: Texture? = null,
        faceCulling: FaceCulling = FaceCulling.DISABLED,
        depthTesting: DepthTesting = DepthTesting.ENABLED
    ) {
        this.configureShader(shader, vertShader, fragShader)
        shader[vertShader.localTransform] = localTransform
        if (texture != null) {
            shader[fragShader.texture] = texture
        }
        this.withInstanceBatches(instances) { batchSize, buff ->
            shader[vertShader.instances] = buff
            geometry.render(
                shader, this.dest,
                instanceCount = batchSize,
                faceCulling, depthTesting
            )
        }
    }

}
