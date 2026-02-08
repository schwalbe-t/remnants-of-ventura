
#include "common/renderer.frag.glsl"
#include "common/shading.frag.glsl"

in vec3 fPosWorld;
in vec3 fNormal;
in vec2 fTexCoords;

out vec4 oColor;

void main(void) {
    vec4 texColor = texture(uTexture, fTexCoords);
    oColor = shadedColor(texColor, fPosWorld, fNormal);
}