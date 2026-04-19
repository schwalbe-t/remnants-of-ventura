
#pragma once
#include "renderer.vert.glsl"

vec4 localToModelPos(vec3 vertPos, uvec4 vBoneIds, vec4 vBoneWeights) {
    vec4 posLocal = uLocalTransform * vec4(vertPos, 1.0);
    vec4 posSkinned
        = (uJointTransforms[vBoneIds[0]] * posLocal) * vBoneWeights[0]
        + (uJointTransforms[vBoneIds[1]] * posLocal) * vBoneWeights[1]
        + (uJointTransforms[vBoneIds[2]] * posLocal) * vBoneWeights[2]
        + (uJointTransforms[vBoneIds[3]] * posLocal) * vBoneWeights[3];
    return posSkinned;
}

vec4 modelToWorldPos(vec4 modelPos) {
    return instances[gl_InstanceID] * modelPos;
}

vec3 localToModelNormal(vec3 vNormal, uvec4 vBoneIds, vec4 vBoneWeights) {
    vec3 normLocal = mat3(uLocalTransform) * vNormal;
    vec3 normSkinned
        = (mat3(uJointTransforms[vBoneIds[0]]) * vNormal) * vBoneWeights[0]
        + (mat3(uJointTransforms[vBoneIds[1]]) * vNormal) * vBoneWeights[1]
        + (mat3(uJointTransforms[vBoneIds[2]]) * vNormal) * vBoneWeights[2]
        + (mat3(uJointTransforms[vBoneIds[3]]) * vNormal) * vBoneWeights[3];
    return normSkinned;
}

vec3 modelToWorldNormal(vec3 modelNormal) {
    return mat3(instances[gl_InstanceID]) * modelNormal;
}
