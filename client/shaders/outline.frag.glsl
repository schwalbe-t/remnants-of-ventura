
#include "common/renderer.frag.glsl"
#include "common/placeholders.frag.glsl"
#include "common/shading.frag.glsl"

in vec2 fTexCoords;

out vec4 oColor;

void main(void) {
    vec4 texColor = texture(uTexture, fTexCoords);
    #ifndef PRESERVE_TRANSPARENT
        if (texColor.a == 0.0) { discard; }
    #endif
    #ifdef OUTLINE_COLOR_OVERRIDE
        oColor = OUTLINE_COLOR_OVERRIDE;
    #else
        oColor = outlineColor(withPlaceholders(texColor));
    #endif
}