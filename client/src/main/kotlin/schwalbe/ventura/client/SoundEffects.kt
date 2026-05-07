
package schwalbe.ventura.client

import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.audio.Audio
import schwalbe.ventura.engine.audio.loadOgg

object SoundEffects {

    val DEEP_WOOSH = Audio.loadOgg("res/sounds/deep_woosh.ogg")
    val DOOR = Audio.loadOgg("res/sounds/door.ogg")
    val ERROR = Audio.loadOgg("res/sounds/error.ogg")
    val ITEM_PICKUP = Audio.loadOgg("res/sounds/item_pickup.ogg")
    val LASER = Audio.loadOgg("res/sounds/laser.ogg")
    val POP = Audio.loadOgg("res/sounds/pop.ogg")
    val PRINTER = Audio.loadOgg("res/sounds/printer.ogg")
    val ROBOT_DESTRUCT = Audio.loadOgg("res/sounds/robot_destruct.ogg")
    val ROBOT_REPAIR = Audio.loadOgg("res/sounds/robot_repair.ogg")

    fun submitResources(loader: ResourceLoader): Unit = loader.submitAll(
        DEEP_WOOSH, DOOR, ERROR, ITEM_PICKUP, LASER,
        POP, PRINTER, ROBOT_DESTRUCT, ROBOT_REPAIR
    )

}
