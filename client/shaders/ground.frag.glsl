
#include "common/shading.frag.glsl"

in vec3 fPosWorld;
in vec3 fColor;

out vec4 oColor;

const vec3 NORMAL = vec3(0.0, +1.0, 0.0);

void main(void) {
    oColor = shadedColor(vec4(fColor, 1.0), fPosWorld, NORMAL);
}