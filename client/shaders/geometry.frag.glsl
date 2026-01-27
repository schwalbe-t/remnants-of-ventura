
#include "common/renderer.frag.glsl"

in vec3 fNormal;
in vec2 fTexCoords;

out vec4 oColor;

void main(void) {
    oColor = texture(uTexture, fTexCoords);
    if (oColor.a == 0.0) { discard; }
}