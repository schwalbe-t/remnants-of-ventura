
package schwalbe.ventura.engine.ui

import org.joml.*

interface Colored {
    
    var color: Vector4fc
    
}

fun <E : Colored> E.withColor(color: Vector4fc) : E {
    this.color = color
    return this
}

fun <E : Colored> E.withColor(color: Vector3fc, alpha: Float = 1f): E {
    this.color = Vector4f(color, alpha)
    return this
}

fun <E : Colored> E.withColor(r: Int, g: Int, b: Int, a: Int = 255): E {
    this.color = Vector4f(r / 255f, g / 255f, b / 255f, a / 255f)
    return this
}