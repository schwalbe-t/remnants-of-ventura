
layout(location = 0) in vec2 vPosition;
layout(location = 1) in vec2 vTexCoords;

layout(std140) uniform uInstances {
    vec2 instances[16];
};

out vec2 fTexCoords;

void main(void) {
    vec2 pos = vPosition + instances[gl_InstanceID];
    gl_Position = vec4(pos, 0.0, 1.0);
    fTexCoords = vTexCoords;
}