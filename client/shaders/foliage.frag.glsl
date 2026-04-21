
#include "common/renderer.frag.glsl"
#include "common/shading.frag.glsl"
#include "foliage.h.glsl"

in vec3 fPosModel;
in vec3 fPosWorld;
in vec3 fNormal;
in vec2 fTexCoords;

uniform vec3 uBottomColor;
uniform vec3 uTopColor;

out vec4 oColor;

void main(void) {
    vec4 texColor = texture(uTexture, fTexCoords);
    if (texColor.a == 0.0) { discard; }
    float normHeight = foliageHeightAt(fPosModel.y);
    vec3 gradientColor = mix(uBottomColor, uTopColor, normHeight);
    oColor = shadedColor(vec4(gradientColor, 1.0), fPosWorld, fNormal);
}