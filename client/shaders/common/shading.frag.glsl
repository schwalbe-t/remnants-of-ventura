
#pragma once
#include "renderer.frag.glsl"
#include "projection.h.glsl"

vec4 baseShadedColor(vec4 texColor) {
    vec4 baseShadedColor = texColor;
    baseShadedColor.rgb *= uBaseFactor;
    return baseShadedColor;
}

float diffuseIntensityOf(vec3 normal) {
    return dot(normalize(normal), uGroundToSun);
}

#define DIFFUSE_THRESHOLD 0.0

bool isInDiffuseShadow(vec3 normal) {
    float diffuse = diffuseIntensityOf(normal);
    return diffuseIntensityOf(normal) <= DIFFUSE_THRESHOLD;
}

float mappedShadowStrength(vec3 posWorld, vec3 normal) {
    vec3 posSurface = posWorld + normal * uNormalOffset;
    vec4 posClip = uSunViewProjection * vec4(posSurface, 1.0);
    vec3 posNorm = ndcToNorm(clipToNdc(posClip));
    bool inBounds =
        all(greaterThanEqual(posNorm.xy, vec2(0.0))) &&
        all(lessThanEqual(posNorm.xy, vec2(1.0)));
    if (!inBounds) { return float(!uDefaultLit); }
    ivec2 texelCoord = ivec2(posNorm.xy * vec2(textureSize(uShadowMap)));
    float shadow = 0.0;
    for (int sampleI = 0; sampleI < uShadowMapSamples; sampleI += 1) {
        float mappedDepth = texelFetch(uShadowMap, texelCoord, sampleI).r;
        shadow += float(posNorm.z > mappedDepth + uDepthBias);
    }
    return shadow / float(uShadowMapSamples);
}

vec4 shadedColor(vec4 texColor, vec3 posWorld, vec3 normal) {
    vec4 baseColor = baseShadedColor(texColor);
    float shadowStrength = isInDiffuseShadow(normal) ? 1.0
        : mappedShadowStrength(posWorld, normal);
    vec4 shadowColor = baseColor;
    shadowColor.rgb *= mix(vec3(1.0), uShadowFactor, shadowStrength);
    return shadowColor;
}

vec4 outlineColor(vec4 texColor) {
    vec4 baseColor = baseShadedColor(texColor);
    vec4 outlineColor = baseColor;
    outlineColor.rgb *= uOutlineFactor;
    return outlineColor;
}