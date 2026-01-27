
#include "common/renderer.frag.glsl"

in vec3 fNormal;
in vec2 fTexCoords;

out vec4 oColor;

float diffuseIntensityOf(vec3 normal) {
    return dot(normalize(normal), uGroundToSun);
}

void main(void) {
    float diffuse = diffuseIntensityOf(fNormal);
    oColor = texture(uTexture, fTexCoords);
    if (oColor.a == 0.0) { discard; }
    oColor.rgb *= uBaseFactor;
    if (diffuse <= 0.0) {
        oColor.rgb *= uShadowFactor;
    }
}