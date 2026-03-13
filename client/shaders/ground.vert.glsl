
#include "common/renderer.vert.glsl"

layout(location = 0) in vec3 vPosition;
layout(location = 1) in vec3 vColor;

out vec3 fPosWorld;
out vec3 fColor;

void main(void) {
    vec4 posWorld
        = instances[gl_InstanceID]
        * uLocalTransform
        * vec4(vPosition, 1.0);
    fPosWorld = posWorld.xyz;
    gl_Position = uViewProjection * posWorld;
    fColor = vColor;
}