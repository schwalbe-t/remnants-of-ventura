
layout(location = 0) in vec3 vPosition;
layout(location = 1) in vec3 vNormal;
layout(location = 2) in vec2 vTexCoords;
layout(location = 4) in uvec4 vBoneIds;
layout(location = 5) in vec4 vBoneWeights;

uniform mat4 uLocalTransform;
uniform mat4 uModelTransform;
uniform mat4 uViewProjection;
uniform mat4 uJointTransforms[64];

out vec3 fNormal;
out vec2 fTexCoords;

void main(void) {
    // mat4 skinTransform
    //     = (uJointTransforms[vBoneIds[0]] * vBoneWeights[0])
    //     + (uJointTransforms[vBoneIds[1]] * vBoneWeights[1])
    //     + (uJointTransforms[vBoneIds[2]] * vBoneWeights[2])
    //     + (uJointTransforms[vBoneIds[3]] * vBoneWeights[3]);
    mat4 skinTransform = mat4(1.0);
    gl_Position
        = uViewProjection
        * uModelTransform
        * uLocalTransform
        * skinTransform
        * vec4(vPosition, 1.0);
    fTexCoords = vTexCoords;
    fNormal
        = mat3(uModelTransform)
        * mat3(uLocalTransform)
        * mat3(skinTransform)
        * vNormal;
}