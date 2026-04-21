
#include "common/renderer.vert.glsl"
#include "common/spaces.vert.glsl"
#include "common/constants.h.glsl"
#include "foliage.h.glsl"

layout(location = 0) in vec3 vPosition;
layout(location = 1) in vec3 vNormal;
layout(location = 2) in vec2 vTexCoords;
layout(location = 3) in uvec4 vBoneIds;
layout(location = 4) in vec4 vBoneWeights;

uniform float uSwayInterval;
uniform float uSwayAmount;
uniform sampler2D uPerlinMap;

out vec3 fPosModel;
out vec3 fPosWorld;
out vec3 fNormal;
out vec2 fTexCoords;

float angleAt(vec2 pos) {
    return texture(uPerlinMap, mod(pos, 1.0)).r * TAU;
}

const float WIND_SPEED = 0.5;
const vec2 WIND_DIRECTION = vec2(+0.5, +0.25);

vec3 swayOffsetAt(vec4 posModel, vec4 posWorld) {
    float normHeight = foliageHeightAt(posModel.y);
    vec2 windOffset = WIND_DIRECTION * uTime * WIND_SPEED;
    posWorld.xz += windOffset;
    float angle = angleAt(posWorld.xz / 21.34);
    float sway = normHeight * sin(uTime) * uSwayAmount;
    return vec3(cos(angle) * sway, 0.0, sin(angle) * sway);
}

void main(void) {
    vec4 posModel = localToModelPos(vPosition, vBoneIds, vBoneWeights);
    fPosModel = posModel.xyz;
    vec4 posWorld = modelToWorldPos(posModel);
    posWorld.xyz += swayOffsetAt(posModel, posWorld);
    fPosWorld = posWorld.xyz;
    gl_Position = uViewProjection * posWorld;
    fTexCoords = vTexCoords;
    vec3 normalModel = localToModelNormal(vNormal, vBoneIds, vBoneWeights);
    vec3 normalWorld = modelToWorldNormal(normalModel);
    fNormal = normalize(normalWorld);
}