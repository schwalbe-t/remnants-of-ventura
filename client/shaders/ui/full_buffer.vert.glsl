
layout(location = 0) in vec2 vTexCoords;

out vec2 fTexCoords;
out vec2 fPosUv;

void main() {
    fTexCoords = vTexCoords;
    fPosUv = vTexCoords;
    vec2 posNdc = vTexCoords * 2.0 - vec2(1.0, 1.0);
    gl_Position = vec4(posNdc, 0.0, 1.0);
}