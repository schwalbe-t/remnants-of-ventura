
layout(location = 0) in vec3 vPosition;
layout(location = 1) in vec3 vNormal;
layout(location = 2) in vec2 vTexCoords;
layout(location = 4) in uvec4 vBoneIds;
layout(location = 5) in vec4 vBoneWeights;

uniform mat4 uLocalTransform;
uniform mat4 uModelTransform;
uniform mat4 uViewProjection;

out vec3 fNormal;
out vec2 fTexCoords;

void main(void) {
    gl_Position
        = uViewProjection
        * uModelTransform
        * uLocalTransform
        * vec4(vPosition, 1.0);
    fTexCoords = vTexCoords;
    fNormal
        = mat3(uModelTransform)
        * mat3(uLocalTransform)
        * vNormal;
}