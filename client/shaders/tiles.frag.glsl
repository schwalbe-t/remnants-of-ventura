
#include "common/shading.frag.glsl"

in vec3 fPosWorld;
in vec3 fColor;

out vec4 oColor;

const vec3 NORMAL = vec3(0.0, +1.0, 0.0);
const float THICKNESS = 0.05;
const vec3 LINE_COLOR = vec3(0.0, 0.0, 0.0);

void main(void) {
    vec2 onLine = round(fPosWorld.xz);
    vec2 toLine = fPosWorld.xz - onLine;
    float distToLine = min(abs(toLine.x), abs(toLine.y));
    oColor = shadedColor(vec4(fColor, 1.0), fPosWorld, NORMAL);
    if (distToLine * 2 <= THICKNESS) {
        oColor.rgb = oColor.rgb * uShadowFactor;
        gl_FragDepth = 0.0;
    } else {
        gl_FragDepth = gl_FragCoord.z;
    }
}