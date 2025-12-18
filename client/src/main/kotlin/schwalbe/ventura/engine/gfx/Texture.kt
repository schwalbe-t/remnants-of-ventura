
package schwalbe.ventura.engine.gfx

import schwalbe.ventura.engine.UsageAfterDisposalException
import schwalbe.ventura.engine.Disposable

import org.lwjgl.opengl.GL33.*

class Texture : Bindable, Disposable {

    var texId: Int? = null
        private set

    val width: Int = 0
    val height: Int = 0

    fun getTexId(): Int
        = this.texId ?: throw UsageAfterDisposalException()

    override fun bind() {

    }

    override fun dispose() {

    }

}