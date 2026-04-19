
#include "common/renderer.vert.glsl"
#include "common/spaces.vert.glsl"

layout(location = 0) in vec3 vPosition;
layout(location = 1) in vec3 vNormal;
layout(location = 2) in vec2 vTexCoords;
layout(location = 3) in uvec4 vBoneIds;
layout(location = 4) in vec4 vBoneWeights;

out vec3 fPosModel;
out vec3 fPosWorld;
out vec3 fNormal;
out vec2 fTexCoords;

void main(void) {
    vec4 posModel = localToModelPos(vPosition, vBoneIds, vBoneWeights);
    fPosModel = posModel.xyz;
    vec4 posWorld = modelToWorldPos(posModel);
    fPosWorld = posWorld.xyz;
    gl_Position = uViewProjection * posWorld;
    fTexCoords = vTexCoords;
    vec3 normalModel = localToModelNormal(vNormal, vBoneIds, vBoneWeights);
    vec3 normalWorld = modelToWorldNormal(normalModel);
    fNormal = normalize(normalWorld);
}