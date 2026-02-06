
package schwalbe.ventura.client

import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.gfx.*
import schwalbe.ventura.net.toVector3f
import schwalbe.ventura.data.RendererConfig
import org.joml.Matrix4f
import org.joml.Matrix4fc
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.collections.toMutableList

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
            geometryShader, outlineShader
        )
    }


    val camera = Camera()

    var config: RendererConfig = RendererConfig.default

    var viewProj: Matrix4fc = Matrix4f()
        private set

    private val instances = UniformBuffer(BufferWriteFreq.EVERY_FRAME)
    private val instanceBuff
        = ByteBuffer.allocateDirect(
            RendererVert.MAX_NUM_INSTANCES *
                    16 /* num floats in mat4 */ * 4 /* sizeof(float) */
        )
        .order(ByteOrder.nativeOrder())

    fun update() {
        this.viewProj = this.camera.computeViewProj(this.dest)
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
            geometryShader(), GeometryVert.renderer, GeometryFrag.renderer,
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
        val shader = outlineShader()
        shader[OutlineVert.outlineThickness] = outlineThickness
        this.render(
            model,
            shader, OutlineVert.renderer, OutlineFrag.renderer,
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
            this.instanceBuff.clear()
            val buff: FloatBuffer = this.instanceBuff.asFloatBuffer()
            for (i in 0..<batch.size) {
                batch[i].get(i * 16, buff)
            }
            this.instances.write(this.instanceBuff)
            f(batchSize, this.instances)
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
        val cfg = this.config
        shader[vertShader.viewProjection] = this.viewProj
        shader[fragShader.baseFactor] = cfg.baseColorFactor.toVector3f()
        shader[fragShader.shadowFactor] = cfg.shadowColorFactor.toVector3f()
        shader[fragShader.outlineFactor] = cfg.outlineColorFactor.toVector3f()
        shader[fragShader.groundToSun] = cfg.groundToSun.toVector3f()
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
        val cfg = this.config
        shader[vertShader.viewProjection] = this.viewProj
        shader[vertShader.localTransform] = localTransform
        if (texture != null) {
            shader[fragShader.texture] = texture
        }
        shader[fragShader.baseFactor] = cfg.baseColorFactor.toVector3f()
        shader[fragShader.shadowFactor] = cfg.shadowColorFactor.toVector3f()
        shader[fragShader.outlineFactor] = cfg.outlineColorFactor.toVector3f()
        shader[fragShader.groundToSun] = cfg.groundToSun.toVector3f()
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
