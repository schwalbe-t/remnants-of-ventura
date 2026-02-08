
#include "common/renderer.vert.glsl"

layout(location = 0) in vec2 vTexCoords;

out vec3 fPosWorld;

void main(void) {
    mat4 instance = instances[gl_InstanceID];
    vec4 posHomo = vec4(vTexCoords.x - 0.5, 0.0, vTexCoords.y - 0.5, 1.0);
    vec4 posWorld = instances[gl_InstanceID] * uLocalTransform * posHomo;
    fPosWorld = posWorld.xyz;
    gl_Position = uViewProjection * posWorld;
}