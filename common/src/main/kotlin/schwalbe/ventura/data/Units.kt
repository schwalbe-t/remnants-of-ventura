
package schwalbe.ventura.data

import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.floor

const val UNITS_PER_CHUNK: Int = 16

fun Int.chunksToUnits(): Int
    = this * UNITS_PER_CHUNK
fun Int.unitsToChunks(): Int
    = this.floorDiv(UNITS_PER_CHUNK)

fun Float.chunksToUnits(): Float
    = this * UNITS_PER_CHUNK
fun Float.unitsToChunks(): Float
    = this / UNITS_PER_CHUNK
fun Float.chunksToChunkIdx(): Int
    = floor(this).toInt()
fun Float.unitsToChunkIdx(): Int
    = floor(this / UNITS_PER_CHUNK).toInt()

fun Vector2f.chunksToUnits(): Vector2f
    = this.mul(UNITS_PER_CHUNK.toFloat())
fun Vector2f.unitsToChunks(): Vector2f
    = this.div(UNITS_PER_CHUNK.toFloat())

fun Vector3f.chunksToUnits(): Vector3f
    = this.mul(UNITS_PER_CHUNK.toFloat())
fun Vector3f.unitsToChunks(): Vector3f
    = this.div(UNITS_PER_CHUNK.toFloat())
