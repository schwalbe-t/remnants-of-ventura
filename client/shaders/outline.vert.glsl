
#include "common/renderer.vert.glsl"
#include "common/spaces.vert.glsl"

layout(location = 0) in vec3 vPosition;
layout(location = 1) in vec3 vNormal;
layout(location = 2) in vec2 vTexCoords;
layout(location = 3) in uvec4 vBoneIds;
layout(location = 4) in vec4 vBoneWeights;

uniform float uOutlineThickness;

out vec2 fTexCoords;

void main(void) {
    vec4 posModel = localToModelPos(vPosition, vBoneIds, vBoneWeights);
    vec4 posWorld = modelToWorldPos(posModel);
    vec3 normalModel = localToModelNormal(vNormal, vBoneIds, vBoneWeights);
    vec3 normalWorld = normalize(modelToWorldNormal(normalModel));
    vec4 posOffset = posWorld + vec4(normalWorld * uOutlineThickness, 0.0);
    gl_Position = uViewProjection * posOffset;
    fTexCoords = vTexCoords;
}