
#include "common/renderer.vert.glsl"

layout(location = 0) in vec2 vTexCoords;

void main(void) {
    mat4 instance = instances[gl_InstanceID];
    vec4 posHomo = vec4(vTexCoords.x - 0.5, 0.0, vTexCoords.y - 0.5, 1.0);
    gl_Position
        = uViewProjection
        * instances[gl_InstanceID]
        * uLocalTransform
        * posHomo;
}