
#include "../common/constants.glsl"

in vec2 fPosUv;

uniform float uSigma;
uniform int uKernelRadius;

out vec4 oColor;

void main() {
    vec2 posNdc = (fPosUv * 2.0) - vec2(1.0, 1.0);
    vec2 posRelPx = posNdc * float(uKernelRadius);
    float x = posRelPx.x;
    float y = posRelPx.y;
    float weight = exp(-(x*x + y*y) / (2.0 * uSigma*uSigma));
    oColor = vec4(weight, 0.0, 0.0, 0.0);
}