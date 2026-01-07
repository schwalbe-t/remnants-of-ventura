
package schwalbe.ventura.engine.gfx

import org.joml.*

class Animation<A : Model.Animations<A>>(val name: String) {
    
    data class Frame<T>(
        val time: Float,
        val value: T
    )
    
    data class Channel(
        val position: List<Frame<Vector3fc>>,
        val rotation: List<Frame<Quaternionfc>>,
        val scale: List<Frame<Vector3fc>>
    )
    
    var lengthS: Float = 0f
        private set
        
    var channels: Map<String, Channel> = mapOf()
        private set
        
    fun load(lengthS: Float, channels: Map<String, Channel>) {
        this.lengthS = lengthS
        this.channels = channels
    }
    
}