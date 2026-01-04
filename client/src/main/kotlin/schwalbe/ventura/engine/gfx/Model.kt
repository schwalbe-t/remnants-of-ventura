
package schwalbe.ventura.engine.gfx

import schwalbe.ventura.engine.Resource
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.PointerBuffer
import org.lwjgl.assimp.Assimp.Functions.*
import org.lwjgl.assimp.Assimp.*
import org.lwjgl.assimp.*
import java.nio.IntBuffer

class Model {
    
    enum class IndexType { SHORT, INT }
    
    enum class Property(val attribute: Geometry.Attribute) {
        POSITION        (Geometry.float (3)),
        NORMAL          (Geometry.float (3)),
        UV              (Geometry.float (2)),
        BONE_IDS_BYTE   (Geometry.ubyte (4)),
        BONE_IDS_SHORT  (Geometry.ushort(4)),
        BONE_WEIGHTS    (Geometry.float (4))
    }
    
    companion object
    
    
    // TODO!
    
}

private fun <T> PointerBuffer?.wrap(create: (Long) -> T): List<T> =
    if (this == null) { listOf() }
    else { (0..<this.capacity()).map { i -> create(this.get(i)) } }

private const val ASSIMP_FLAGS: Int =
    aiProcess_Triangulate or 
    aiProcess_JoinIdenticalVertices or
    aiProcess_LimitBoneWeights or
    aiProcess_ValidateDataStructure or
    aiProcess_GenNormals
    
fun Model.Companion.loadFile(
    path: String, properties: List<Model.Property>,
    indexType: Model.IndexType = Model.IndexType.SHORT
): Resource<Model> = Resource {
    val scene: AIScene? = aiImportFile(path, ASSIMP_FLAGS)
    check(scene != null) {
        val error: String = aiGetErrorString() ?: "<unknown error>"
        "Model file '$path' could not be read:\n$error"
    }
    return@Resource {
        val texType: Int = aiTextureType_EMISSIVE
        for (material in scene.mMaterials().wrap(AIMaterial::create)) {
            if (aiGetMaterialTextureCount(material, texType) == 0) { continue }
            val path: AIString = AIString.create()
            aiGetMaterialTexture(
                material, texType, 0, path,
                null, null, null, null, null, null as IntBuffer?
            )
            println("Material uses texture ${path.dataString()}")
        }
        for ((i, t) in scene.mTextures().wrap(AITexture::create).withIndex()) {
            if (t.mHeight() == 0) {
                println("Texture [$i] exists as ${t.mWidth()} byte(s) of encoded image data")
            } else {
                println("Texture [$i] exists as decoded ARGB8 image with width ${t.mWidth()} and height ${t.mHeight()}")
            }
        }
        aiReleaseImport(scene)
        throw NotImplementedError("not yet implemented")
    }
}