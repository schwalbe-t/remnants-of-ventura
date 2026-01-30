
in vec2 fPosUv;

uniform sampler2D uBackground;
uniform vec2 uBackgroundSizePx;
uniform vec2 uBufferSizePx;
uniform vec2 uAbsOffsetPx;

uniform int uKernelRadius;
uniform int uKernelSpread;
uniform sampler2D uKernelWeights;

out vec4 oColor;

vec2 uvToPx(vec2 posUv, vec2 size) {
    float xPosPx = posUv.x * size.x;
    float yPosPx = (1.0 - posUv.y) * size.y;
    return vec2(xPosPx, yPosPx);
}

vec2 pxToUv(vec2 posPx, vec2 size) {
    float xPosUv = posPx.x / size.x;
    float yPosUv = 1.0 - (posPx.y / size.y);
    return vec2(xPosUv, yPosUv);
}

float blurWeightAt(int x, int y) {
    ivec2 kPosPx = ivec2(x + uKernelRadius, y + uKernelRadius);
    return texelFetch(uKernelWeights, kPosPx, 0).r;
}

void main() {
    ivec2 absPosPx = ivec2(uAbsOffsetPx + uvToPx(fPosUv, uBufferSizePx));
    vec4 colorSum = vec4(0.0, 0.0, 0.0, 0.0);
    float weightSum = 0.0;
    for (int rx = -uKernelRadius; rx <= uKernelRadius; rx += 1)
    for (int ry = -uKernelRadius; ry <= uKernelRadius; ry += 1) {
        float weight = blurWeightAt(rx, ry);
        ivec2 samplePos = absPosPx + ivec2(rx, ry) * uKernelSpread;
        vec2 posUv = pxToUv(samplePos, uBackgroundSizePx);
        colorSum += weight * texture(uBackground, posUv);
        weightSum += weight;
    }
    oColor = colorSum / weightSum;
}