
layout(location = 0) in vec2 vTexCoords;

uniform vec2 uBufferSizePx;
uniform vec2 uDestTopLeftPx;
uniform vec2 uDestSizePx;

out vec2 fTexCoords;
out vec2 fPosUv;

void main() {
    fTexCoords = vTexCoords;
    // *PosPx = position of the vertex in pixels (top left origin, y- = up)
    float xPosPx = uDestTopLeftPx.x + vTexCoords.x * uDestSizePx.x;
    float yPosPx = uDestTopLeftPx.y + (1.0 - vTexCoords.y) * uDestSizePx.y;
    // *PosUv = position in 0..1 range (bottom left origin, y+ = up)
    float xPosUv = xPosPx / uBufferSizePx.x;
    float yPosUv = 1.0 - (yPosPx / uBufferSizePx.y);
    fPosUv = vec2(xPosUv, yPosUv);
    // gl_Position = position in -1..+1 range (center origin, y+ = up)
    vec2 posNdc = vec2(xPosUv, yPosUv) * 2.0 - vec2(1.0, 1.0);
    gl_Position = vec4(posNdc, 0.0, 1.0);
}