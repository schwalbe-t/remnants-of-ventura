
package schwalbe.ventura.client.game

import schwalbe.ventura.client.Camera
import schwalbe.ventura.engine.gfx.*


class Renderer {

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
    }


    val camera = Camera()
    val sun = Camera()



}
