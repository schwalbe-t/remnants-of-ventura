
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

    private val icons: MutableList<Icon> = mutableListOf()
    private fun create(name: String): Icon {
        val icon = Icon("res/icons/$name")
        this.icons.add(icon)
        return icon
    }

    val ADD = create("add.png")
    val CHECK_BOX_BLANK = create("check_box_blank.png")
    val CHECK_BOX_CHECKED = create("check_box_checked.png")
    val DELETE = create("delete.png")
    val EDIT = create("edit.png")
    val PLAY = create("play.png")
    val POWER = create("power.png")
    val SETTINGS = create("settings.png")
    val TRANSLATE = create("translate.png")

    fun submitResources(resLoader: ResourceLoader) {
        this.icons.forEach { resLoader.submit(it.image) }
    }

}
