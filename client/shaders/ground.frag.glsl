
#include "common/shading.frag.glsl"

uniform vec4 uGroundColor;

out vec4 oColor;

const vec3 NORMAL = vec3(0.0, +1.0, 0.0);

void main(void) {
    oColor = shadedColor(uGroundColor, NORMAL);
}