
#include "common/renderer.frag.glsl"
#include "common/shading.frag.glsl"

in vec2 fTexCoords;

out vec4 oColor;

void main(void) {
    vec4 texColor = texture(uTexture, fTexCoords);
    if (texColor.a == 0.0) { discard; }
    oColor = outlineColor(texColor);
}