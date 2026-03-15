
// The purpose of this shader is to replace the provided fragment shader
// in the shadow rendering pass, since it requires no color output.

#include "common/renderer.frag.glsl"

in vec2 fTexCoords;

void main(void) {
    vec4 texColor = texture(uTexture, fTexCoords);
    if (texColor.a == 0.0) { discard; }
    // depth is written automatically
}
