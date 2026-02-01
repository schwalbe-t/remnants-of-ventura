
#include "common/renderer.vert.glsl"

layout(location = 0) in vec3 vPosition;
layout(location = 1) in vec3 vNormal;
layout(location = 2) in vec2 vTexCoords;
layout(location = 3) in uvec4 vBoneIds;
layout(location = 4) in vec4 vBoneWeights;

uniform float uOutlineThickness;

out vec2 fTexCoords;

void main(void) {
    mat4 instance = instances[gl_InstanceID];
    vec4 posHomo = vec4(vPosition, 1.0);
    vec4 posSkinned
        = (uJointTransforms[vBoneIds[0]] * posHomo) * vBoneWeights[0]
        + (uJointTransforms[vBoneIds[1]] * posHomo) * vBoneWeights[1]
        + (uJointTransforms[vBoneIds[2]] * posHomo) * vBoneWeights[2]
        + (uJointTransforms[vBoneIds[3]] * posHomo) * vBoneWeights[3];
    vec3 normalSkinned
        = (mat3(uJointTransforms[vBoneIds[0]]) * vNormal) * vBoneWeights[0]
        + (mat3(uJointTransforms[vBoneIds[1]]) * vNormal) * vBoneWeights[1]
        + (mat3(uJointTransforms[vBoneIds[2]]) * vNormal) * vBoneWeights[2]
        + (mat3(uJointTransforms[vBoneIds[3]]) * vNormal) * vBoneWeights[3];
    vec4 posWorld = instance * uLocalTransform * posSkinned;
    vec3 normWorld = mat3(instance) * mat3(uLocalTransform) * normalSkinned;
    vec3 normWorldNorm = normalize(normWorld);
    vec4 posOffset = posWorld + vec4(normWorldNorm * uOutlineThickness, 0.0);
    gl_Position = uViewProjection * posOffset;
    fTexCoords = vTexCoords;
}