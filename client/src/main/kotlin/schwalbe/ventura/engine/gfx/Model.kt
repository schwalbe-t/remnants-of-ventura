
package schwalbe.ventura.engine.gfx

import schwalbe.ventura.engine.Resource
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.PointerBuffer
import org.lwjgl.assimp.Assimp.Functions.*
import org.lwjgl.assimp.Assimp.*
import org.lwjgl.assimp.*
import org.joml.*
import java.io.IOException
import java.nio.*
import java.nio.file.Files
import java.nio.file.Paths

class Model<A : Model.Animations<A>>(
    val rootNode: Node?,
    val nodes: Map<String, Node>,
    val meshes: List<Mesh>
) {
    
    enum class IndexType(val numBytes: Int) {
        SHORT(2),
        INT  (4) 
    }
    
    enum class Property(val attribute: Geometry.Attribute) {
        POSITION        (Geometry.float (3)),
        NORMAL          (Geometry.float (3)),
        UV              (Geometry.float (2)),
        BONE_IDS_BYTE   (Geometry.ubyte (4)),
        BONE_IDS_SHORT  (Geometry.ushort(4)),
        BONE_WEIGHTS    (Geometry.float (4))
    }
    
    data class Bone(
        val name: String,
        val inverseBind: Matrix4fc
    )
    
    data class Mesh(
        val geometry: Geometry,
        val texture: Texture,
        val bones: List<Bone>
    )
    
    data class Node(
        val localTransform: Matrix4fc,
        val meshes: List<Int>,
        val children: List<Node>
    )
    
    abstract class Animations<A : Animations<A>> {
        
        val byName: MutableMap<String, Animation<A>> = mutableMapOf()
        
        fun anim(name: String): Animation<A> {
            val a = Animation<A>(name)
            this.byName[name] = a
            return a
        }
        
    }
    
    companion object

    
    fun forEachNode(n: Node? = this.rootNode, f: (Node) -> Unit) {
        if (n == null) { return }
        f(n)
        n.children.forEach { this.forEachNode(it, f) }
    }
    
    fun <V : VertShaderDef<V>, F : FragShaderDef<F>> render(
        shader: Shader<V, F>, framebuffer: ConstFramebuffer,
        localTransform: Uniform<V, Matrix4fc>? = null,
        texture: Uniform<F, Texture>? = null,
        jointTransforms: Uniform<V, Iterable<Matrix4fc>>? = null,
        instanceCount: Int = 1,
        faceCulling: FaceCulling = FaceCulling.DISABLED,
        depthTesting: DepthTesting = DepthTesting.ENABLED
    ) {
        this.forEachNode { node ->
            for (meshI in node.meshes) {
                val mesh: Mesh = this.meshes[meshI]
                if (localTransform != null) {
                    shader[localTransform] =
                        if (!mesh.bones.isEmpty()) { Matrix4f() }
                        else { node.localTransform }
                }
                if (texture != null) {
                    shader[texture] = mesh.texture
                }
                if (jointTransforms != null) {
                    shader[jointTransforms] = mesh.bones.map { Matrix4f() }
                }
                mesh.geometry.render(
                    shader, framebuffer, instanceCount,
                    faceCulling, depthTesting
                )
            }
        }
    }

}

object StaticAnim : Model.Animations<StaticAnim>()
typealias StaticModel = Model<StaticAnim>


private fun <T> PointerBuffer?.map(create: (Long) -> T): List<T> =
    if (this == null) { listOf() }
    else { (0..<this.capacity()).map { i -> create(this.get(i)) } }
    
private fun IntBuffer?.collect(): List<Int> {
    if (this == null) { return listOf() }
    val out = mutableListOf<Int>()
    while (this.remaining() >= 1) {
        out.add(this.get())
    }
    return out
}

private fun toJomlMatrix4(m: AIMatrix4x4) = Matrix4f(
    m.a1(), m.b1(), m.c1(), m.d1(),
    m.a2(), m.b2(), m.c2(), m.d2(),
    m.a3(), m.b3(), m.c3(), m.d3(),
    m.a4(), m.b4(), m.c4(), m.d4()
)

private fun toJomlVector3(v: AIVector3D)
    = Vector3f(v.x(), v.y(), v.z())
    
private fun toJomlQuaternion(q: AIQuaternion)
    = Quaternionf(q.x(), q.y(), q.z(), q.w())

private data class SceneInfo(
    val path: String,
    val properties: List<Model.Property>,
    val layout: List<Geometry.Attribute>,
    val textureFilter: Texture.Filter,
    val indexType: Model.IndexType,
    val materials: List<AIMaterial>,
    val textures: List<AITexture>,
    val nodesByName: MutableMap<String, Model.Node>
)

private fun walkNode(
    node: AINode, parentTransform: Matrix4fc, sceneInfo: SceneInfo
): Model.Node {
    val name: String = node.mName().dataString()
    val localTransform: Matrix4f = toJomlMatrix4(node.mTransformation())
    parentTransform.mul(localTransform, localTransform)
    val meshes: List<Int> = node.mMeshes().collect()
    val children: List<Model.Node> = node.mChildren().map(AINode::create)
        .map { walkNode(it, localTransform, sceneInfo) }
    val node = Model.Node(localTransform, meshes, children)
    check(sceneInfo.nodesByName.put(name, node) == null) {
        "Duplicate node name '$name' in '${sceneInfo.path}'"
    }
    sceneInfo.nodesByName[name] = node
    return node
}

private data class BoneInfo(
    val bone: Model.Bone,
    val weights: Map<Int, Float>
)

private data class RawGeometry(
    val vertexBuffer: ByteBuffer,
    val indexBuffer: ByteBuffer
)

private fun createRawMeshGeometry(
    mesh: AIMesh, bones: List<BoneInfo>, sceneInfo: SceneInfo
): RawGeometry {
    val stride: Int = sceneInfo.layout.computeStride()
    val numVertices: Int = mesh.mNumVertices()
    val normals: AIVector3D.Buffer? = mesh.mNormals()
    check(normals != null) {
        "Mesh '${mesh.mName().dataString()}' in '${sceneInfo.path}'" +
            " does not include surface normals"
    }
    check(mesh.mTextureCoords().remaining() >= 1) {
        "Mesh '${mesh.mName().dataString()}' in '${sceneInfo.path}'" +
            " does not include texture coordinates"
    }
    val texCoords: AIVector3D.Buffer = AIVector3D.create(
        mesh.mTextureCoords().get(0), numVertices
    )
    val vertexBuffer = ByteBuffer
        .allocateDirect(numVertices * stride)
        .order(ByteOrder.nativeOrder())
    for (vertexI in 0..<numVertices) {
        val boneWeights: List<Pair<Int, Float>> = bones
            .withIndex()
            .map { (boneI, bone) -> boneI to (bone.weights[vertexI] ?: 0f) }
            .filter { (boneI, weight) -> weight > 0f }
            .sortedBy { (boneI, weight) -> weight }
            .run {
                this.subList(maxOf(this.size - 4, 0), this.size) +
                    List(maxOf(4 - this.size, 0)) { Pair(0, 0f) }
            }
        val weightSum: Float = boneWeights
            .sumOf { (_, weight) -> weight.toDouble() }.toFloat()
        check(weightSum > 0.0) {
            "Sum of weights for vertex [$vertexI] in mesh" +
                " '${mesh.mName().dataString()}' in model '${sceneInfo.path}'" +
                " is not greater than 0"
        }
        for (prop in sceneInfo.properties) { when (prop) {
            Model.Property.POSITION -> {
                val pos: AIVector3D = mesh.mVertices()[vertexI]
                vertexBuffer.putFloat(pos.x())
                vertexBuffer.putFloat(pos.y())
                vertexBuffer.putFloat(pos.z())
            }
            Model.Property.NORMAL -> {
                val norm: AIVector3D = normals[vertexI]
                vertexBuffer.putFloat(norm.x())
                vertexBuffer.putFloat(norm.y())
                vertexBuffer.putFloat(norm.z())
            }
            Model.Property.UV -> {
                val uv: AIVector3D = texCoords[vertexI]
                vertexBuffer.putFloat(uv.x())
                vertexBuffer.putFloat(uv.y())
            }
            Model.Property.BONE_IDS_BYTE -> {
                for ((boneI, _) in boneWeights) {
                    vertexBuffer.put(boneI.toByte())
                }
            }
            Model.Property.BONE_IDS_SHORT -> {
                for ((boneI, _) in boneWeights) {
                    vertexBuffer.putShort(boneI.toShort())
                }
            }
            Model.Property.BONE_WEIGHTS -> {
                for ((_, weight) in boneWeights) {
                    vertexBuffer.putFloat(weight / weightSum)
                }
            }
        } }
    }
    vertexBuffer.flip()
    val numFaces: Int = mesh.mNumFaces()
    val numIndices: Int = numFaces * 3
    val faces: AIFace.Buffer = mesh.mFaces()
    val indexBuffer = ByteBuffer
        .allocateDirect(numIndices * sceneInfo.indexType.numBytes)
        .order(ByteOrder.nativeOrder())
    val shortIndices: ShortBuffer = indexBuffer.asShortBuffer()
    val intIndices: IntBuffer = indexBuffer.asIntBuffer()
    for (faceI in 0..<numFaces) {
        val face: AIFace = faces[faceI]
        check(face.mNumIndices() == 3) {
            "Face [$faceI] in mesh ${mesh.mName().dataString()} in" +
                " '${sceneInfo.path}' is not triangular and instead has" +
                " ${face.mNumIndices()} indices"
        }
        for (index in face.mIndices().collect()) {
            when (sceneInfo.indexType) {
                Model.IndexType.SHORT   -> shortIndices.put(index.toShort())
                Model.IndexType.INT     -> intIndices.put(index)
            }
        }
    }
    // indexBuffer doesn't need flipping because we advance the short/int views
    return RawGeometry(vertexBuffer, indexBuffer)
}

private fun getMaterialName(material: AIMaterial, index: Int): String {
    val rawName = AIString.calloc()
    val result = aiGetMaterialString(
        material, AI_MATKEY_NAME,
        aiTextureType_NONE, 0,
        rawName
    )
    val name: String? = if (result != aiReturn_SUCCESS) { null }
        else { rawName.dataString() }
    rawName.free()
    return if (name != null) { "'$name'" } else { "[$index]" }
}

const val ASSIMP_REQ_TEX_TYPE: Int = aiTextureType_EMISSIVE

private fun loadMeshTexture(mesh: AIMesh, sceneInfo: SceneInfo): ByteBuffer {
    val materialI: Int = mesh.mMaterialIndex()
    val material: AIMaterial = sceneInfo.materials[materialI]
    check(aiGetMaterialTextureCount(material, ASSIMP_REQ_TEX_TYPE) >= 1) {
        val name: String = getMaterialName(material, materialI)
        "Material $name in '${sceneInfo.path}' does not specify a texture" +
            " under type 'EMISSIVE'"
    }
    val rawTexPath = AIString.calloc()
    val result = aiGetMaterialTexture(
        material, ASSIMP_REQ_TEX_TYPE, 0, rawTexPath,
        null, null, null, null, null, null as IntBuffer?
    )
    check(result == aiReturn_SUCCESS) {
        val name: String = getMaterialName(material, materialI)
        "Failed to get texture of material $name in '${sceneInfo.path}'"
    }
    val texPath: String = rawTexPath.dataString()
    rawTexPath.free()
    if (texPath.startsWith("*")) {
        val texI: Int = texPath.substring(1).toInt()
        val tex: AITexture = sceneInfo.textures[texI]
        check(tex.mHeight() == 0) {
            val name: String = getMaterialName(material, materialI)
            "Material $name in '${sceneInfo.path}' uses an embedded" +
                " and uncompressed texture, which is not supported"
        }
        return tex.pcDataCompressed()
    } else {
        val raw: ByteArray
        try {
            raw = Files.readAllBytes(Paths.get(texPath))
        } catch (e: IOException) {
            throw IllegalStateException(
                "Failed to read image file '$texPath' (used by model" +
                    " '${sceneInfo.path}')"
            )
        }
        val buffer = ByteBuffer
            .allocateDirect(raw.size)
            .order(ByteOrder.nativeOrder())
        buffer.put(raw)
        buffer.flip()
        return buffer
    }
}

private data class RawMesh(
    val geometry: RawGeometry,
    val texture: ByteBuffer,
    val bones: List<Model.Bone>
)

private fun createRawMesh(mesh: AIMesh, sceneInfo: SceneInfo): RawMesh {
    val rawBones: List<AIBone> = mesh.mBones().map(AIBone::create)
    val bones: List<BoneInfo> = rawBones.map {
        val name: String = it.mName().dataString()
        val inverseBind: Matrix4fc = toJomlMatrix4(it.mOffsetMatrix())
        val weights: Map<Int, Float> = it.mWeights()
            .associateBy(AIVertexWeight::mVertexId, AIVertexWeight::mWeight)
        BoneInfo(Model.Bone(name, inverseBind), weights)
    }
    val geometry: RawGeometry = createRawMeshGeometry(mesh, bones, sceneInfo)
    val texture: ByteBuffer = loadMeshTexture(mesh, sceneInfo)
    return RawMesh(geometry, texture, bones.map(BoneInfo::bone))
}

private fun completeRawMesh(mesh: RawMesh, sceneInfo: SceneInfo): Model.Mesh {
    val geometry = when (sceneInfo.indexType) {
        Model.IndexType.SHORT -> Geometry(
            sceneInfo.layout, mesh.geometry.vertexBuffer,
            mesh.geometry.indexBuffer.asShortBuffer()
        )
        Model.IndexType.INT -> Geometry(
            sceneInfo.layout, mesh.geometry.vertexBuffer,
            mesh.geometry.indexBuffer.asIntBuffer()
        )
    }
    val texture = Texture
        .loadBytes(mesh.texture, sceneInfo.textureFilter, sceneInfo.path)
    return Model.Mesh(geometry, texture, mesh.bones)
}

private fun loadAnimation(anim: AIAnimation, dest: Animation<*>) {
    var ticksPerSecond: Double = anim.mTicksPerSecond()
    if (ticksPerSecond == 0.0) { ticksPerSecond = 1.0 }
    val lengthS: Float = (anim.mDuration() / ticksPerSecond).toFloat()
    val channels: MutableMap<String, Animation.Channel> = mutableMapOf()
    for (channel in anim.mChannels().map(AINodeAnim::create)) {
        val nodeName: String = channel.mNodeName().dataString()
        channel.mPositionKeys()
        val position = channel.mPositionKeys()?.map {
            Animation.Frame<Vector3fc>(
                it.mTime().toFloat(), toJomlVector3(it.mValue())
            )
        } ?: listOf()
        val rotation = channel.mRotationKeys()?.map {
            Animation.Frame<Quaternionfc>(
                it.mTime().toFloat(), toJomlQuaternion(it.mValue())
            )
        } ?: listOf()
        val scale = channel.mScalingKeys()?.map {
            Animation.Frame<Vector3fc>(
                it.mTime().toFloat(), toJomlVector3(it.mValue())
            )
        } ?: listOf()
        channels[nodeName] = Animation.Channel(position, rotation, scale)
    }
    dest.load(lengthS, channels)
}

private const val ASSIMP_FLAGS: Int =
    aiProcess_Triangulate or 
    aiProcess_JoinIdenticalVertices or
    aiProcess_LimitBoneWeights or
    aiProcess_ValidateDataStructure or
    aiProcess_GenNormals

fun <A : Model.Animations<A>> Model.Companion.loadFile(
    path: String, properties: List<Model.Property>,
    animations: A,
    textureFilter: Texture.Filter = Texture.Filter.NEAREST,
    indexType: Model.IndexType = Model.IndexType.SHORT
): Resource<Model<A>> = Resource {
    val scene: AIScene? = aiImportFile(path, ASSIMP_FLAGS)
    check(scene != null) {
        val error: String = aiGetErrorString() ?: "<unknown error>"
        "Model file '$path' could not be read:\n$error"
    }
    val attributes: List<Geometry.Attribute> = properties
        .map(Model.Property::attribute)
    val materials = scene.mMaterials().map(AIMaterial::create)
    val textures = scene.mTextures().map(AITexture::create)
    val nodesByName: MutableMap<String, Model.Node> = mutableMapOf()
    val sceneInfo = SceneInfo(
        path, properties, attributes, textureFilter, indexType,
        materials, textures,
        nodesByName
    )
    val rawRootNode: AINode? = scene.mRootNode()
    val rootNode: Model.Node? = if (rawRootNode == null) { null }
        else { walkNode(rawRootNode, Matrix4f(), sceneInfo) }
    val rawMeshes: List<RawMesh> = scene.mMeshes()
        .map { createRawMesh(AIMesh.create(it), sceneInfo) }
    for (animation in scene.mAnimations().map(AIAnimation::create)) {
        val name: String = animation.mName().dataString()
        val dest: Animation<A> = animations.byName[name] ?: continue
        loadAnimation(animation, dest)
    }
    return@Resource {
        val meshes: List<Model.Mesh> = rawMeshes
            .map { completeRawMesh(it, sceneInfo) }
        aiReleaseImport(scene)
        Model(rootNode, sceneInfo.nodesByName, meshes)
    }
}

fun Model.Companion.loadFile(
    path: String, properties: List<Model.Property>,
    textureFilter: Texture.Filter = Texture.Filter.NEAREST,
    indexType: Model.IndexType = Model.IndexType.SHORT
): Resource<StaticModel>
    = Model.loadFile(path, properties, StaticAnim, textureFilter, indexType)
