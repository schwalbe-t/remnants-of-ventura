
layout(location = 0) in vec3 vPosLocal;

uniform mat4 uModelTransform;
uniform mat4 uViewProjection;

out vec3 fPosWorld;

void main(void) {
    vec4 posHomo = vec4(vPosLocal, 1.0);
    vec4 posWorld = uModelTransform * posHomo;
    fPosWorld = posWorld.xyz;
    gl_Position = uViewProjection * posWorld;
}
