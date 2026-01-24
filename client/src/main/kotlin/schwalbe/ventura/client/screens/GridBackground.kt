
package schwalbe.ventura.client.screens

import org.joml.Matrix4f
import org.joml.Vector4f
import schwalbe.ventura.client.Camera
import schwalbe.ventura.client.Client
import schwalbe.ventura.client.computeViewProj
import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.gfx.*

object GridVert : VertShaderDef<GridVert> {
    override val path = "shaders/grid.vert.glsl"

    val modelTransform = mat4("uModelTransform")
    val viewProjection = mat4("uViewProjection")
}

object GridFrag : FragShaderDef<GridFrag> {
    override val path = "shaders/grid.frag.glsl"

    val gridResolution = float("uGridResolution")
    val lineRadius = float("uLineRadius")
    val gridColor = vec4("uGridColor")
}

val gridQuad: Resource<Geometry> = Resource { {
    Geometry.fromFloatArray(
        listOf(Geometry.float(3)),
        floatArrayOf(
            -0.5f, 0f, -0.5f, // [0] top left
            +0.5f, 0f, -0.5f, // [1] top right
            -0.5f, 0f, +0.5f, // [2] bottom left
            +0.5f, 0f, +0.5f, // [3] bottom right
        ),
        shortArrayOf(
            0, 2, 3,
            0, 3, 1
        )
    )
} }

fun renderGridBackground(client: Client): () -> Unit {
    val camera = Camera()
    camera.position.set(15.0f, 3.0f, 20.0f)
    camera.lookAt.set(0.0f, -10.0f, 0.0f)
    camera.fov = Math.PI.toFloat() / 4f
    return {
        client.out3d.clearColor(Vector4f(0.8f, 0.8f, 0.8f, 1f))
        client.out3d.clearDepth(1f)
        val shader: Shader<GridVert, GridFrag> = gridShader()
        shader[GridVert.modelTransform] = Matrix4f()
            .scale(50f)
        shader[GridVert.viewProjection] = camera.computeViewProj(client.out3d)
        shader[GridFrag.gridResolution] = 1f
        shader[GridFrag.lineRadius] = 0.005f
        shader[GridFrag.gridColor] = Vector4f(0.0f, 0.0f, 0.0f, 0.15f)
        gridQuad().render(shader, client.out3d)
    }
}
