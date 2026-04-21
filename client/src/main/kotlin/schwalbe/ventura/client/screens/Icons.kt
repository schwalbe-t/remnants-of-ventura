
package schwalbe.ventura.client.screens

import schwalbe.ventura.engine.Resource
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.gfx.Texture
import schwalbe.ventura.engine.gfx.loadImage
import schwalbe.ventura.engine.ui.*

class Icon(path: String, filter: Texture.Filter = Texture.Filter.LINEAR) {

    val image: Resource<Texture> = Texture.loadImage(path, filter)

    fun create(size: UiSize): UiElement = Image()
        .withImage(this.image())
        .withSize(width = size, height = size)

}

object Icons {

    val ADD = Icon("res/icons/add.png")
    val DELETE = Icon("res/icons/delete.png")
    val EDIT = Icon("res/icons/edit.png")
    val PLAY = Icon("res/icons/play.png")
    val POWER = Icon("res/icons/power.png")
    val SETTINGS = Icon("res/icons/settings.png")
    val TRANSLATE = Icon("res/icons/translate.png")

    fun submitResources(resLoader: ResourceLoader): Unit = resLoader.submitAll(
        ADD.image, DELETE.image, EDIT.image, PLAY.image, POWER.image,
        SETTINGS.image, TRANSLATE.image
    )

}
