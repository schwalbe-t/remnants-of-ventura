
package schwalbe.ventura.client.game

import org.joml.Matrix4f
import org.joml.Matrix4fc
import schwalbe.ventura.client.Camera
import schwalbe.ventura.client.computeViewProj
import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.gfx.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

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
            geometryShader
        )
    }


    val camera = Camera()
    val sun = Camera()

    private var viewProj: Matrix4fc = Matrix4f()

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

    fun <V : VertShaderDef<V>, F : FragShaderDef<F>, A : Animations<A>> render(
        model: Model<A>,
        shader: Shader<V, F>,
        vertShader: RendererVert<V>,
        fragShader: RendererFrag<F>,
        animState: AnimState<A>? = null,
        instances: Iterable<Matrix4fc> = listOf(Matrix4f()),
        faceCulling: FaceCulling = FaceCulling.DISABLED,
        depthTesting: DepthTesting = DepthTesting.ENABLED,
        renderedMeshes: Collection<String>? = null
    ) {
        shader[vertShader.viewProjection] = this.viewProj
        var remaining: MutableList<Matrix4fc> = instances.toMutableList()
        while (remaining.isNotEmpty()) {
            val batchSize: Int
                = minOf(remaining.size, RendererVert.MAX_NUM_INSTANCES)
            val batch: MutableList<Matrix4fc> = remaining.subList(0, batchSize)
            this.instanceBuff.clear()
            val buff: FloatBuffer = this.instanceBuff.asFloatBuffer()
            for (i in 0..<batchSize) {
                batch[i].get(i * 16, buff)
            }
            this.instances.write(this.instanceBuff)
            shader[vertShader.instances] = this.instances
            model.render(
                shader, this.dest,
                vertShader.localTransform,
                fragShader.texture,
                vertShader.jointTransforms,
                animState,
                instanceCount = batchSize,
                faceCulling, depthTesting, renderedMeshes
            )
            batch.clear()
        }
    }

}
