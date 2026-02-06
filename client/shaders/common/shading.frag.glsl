
#pragma once
#include "renderer.frag.glsl"

vec4 baseShadedColor(vec4 texColor) {
    vec4 baseShadedColor = texColor;
    baseShadedColor.rgb *= uBaseFactor;
    return baseShadedColor;
}

float diffuseIntensityOf(vec3 normal) {
    return dot(normalize(normal), uGroundToSun);
}

#define DIFFUSE_THRESHOLD 0.0

vec4 diffuseShadedColor(vec4 baseColor, vec3 normal) {
    vec4 diffColor = baseColor;
    float diffuse = diffuseIntensityOf(normal);
    if (diffuseIntensityOf(normal) <= DIFFUSE_THRESHOLD) {
        diffColor.rgb *= uShadowFactor;
    }
    return diffColor;
}

vec4 shadowMappedColor(vec4 diffColor) {
    return diffColor;
}

vec4 shadedColor(vec4 texColor, vec3 normal) {
    vec4 baseColor = baseShadedColor(texColor);
    vec4 diffColor = diffuseShadedColor(baseColor, normal);
    vec4 shadowColor = shadowMappedColor(diffColor);
    return shadowColor;
}

vec4 outlineColor(vec4 texColor) {
    vec4 baseColor = baseShadedColor(texColor);
    vec4 outlineColor = baseColor;
    outlineColor.rgb *= uOutlineFactor;
    return outlineColor;
}