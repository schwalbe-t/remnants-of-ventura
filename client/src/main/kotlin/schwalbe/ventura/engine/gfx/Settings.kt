
package schwalbe.ventura.engine.gfx

import org.lwjgl.opengl.GL33.*

enum class DepthTesting(internal val glApply: () -> Unit) {
    
    DISABLED({ glDisable(GL_DEPTH_TEST) }),
    ENABLED({ glEnable(GL_DEPTH_TEST) });
    
}

enum class FaceCulling(internal val glApply: () -> Unit) {
    
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
    
}