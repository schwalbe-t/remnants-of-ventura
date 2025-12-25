
package schwalbe.ventura.engine.gfx

import org.lwjgl.opengl.GL33.*

enum class DepthTesting(val glApply: () -> Unit) {
    
    DISABLED({ glDisable(GL_DEPTH_TEST) }),
    ENABLED({ glEnable(GL_DEPTH_TEST) });
    
    companion object {
        internal val bound = BindingManager<DepthTesting> { d -> d.glApply() }
    }
    
}

enum class FaceCulling(val glApply: () -> Unit) {
    
    DISABLED({ glDisable(GL_CULL_FACE) }),
    BACK({
        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
    }),
    FRONT({
        glEnable(GL_CULL_FACE)
        glCullFace(GL_FRONT)
    }),
    FRONT_AND_BACK({
        glEnable(GL_CULL_FACE)
        glCullFace(GL_FRONT_AND_BACK)
    });
    
    companion object {
        internal val bound = BindingManager<FaceCulling> { d -> d.glApply() }
    }
    
}