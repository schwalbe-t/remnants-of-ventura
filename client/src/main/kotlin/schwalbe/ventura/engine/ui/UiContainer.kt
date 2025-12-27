
package schwalbe.ventura.engine.ui

interface UiContainer {
    
    val inside: UiElement?
    
    fun setContents(inside: UiElement?)
    
}

fun <C : UiContainer> UiElement.inside(container: C): C {
    container.setContents(this)
    return container
}