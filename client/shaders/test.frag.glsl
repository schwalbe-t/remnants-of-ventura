
#include "common/test.glsl"

in vec2 fTexCoords;

out vec4 oColor;

void main(void) {
    oColor = texture(uTexture, fTexCoords);
}