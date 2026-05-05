
#pragma once

bool colorEq(vec3 a, vec3 b) {
    vec3 diff = abs(a - b);
    return max(max(diff.r, diff.g), diff.b) < 0.01;
}

#ifdef USE_PLACEHOLDERS

uniform vec3 uPlaceholderColors[PLACEHOLDER_COUNT];
const vec3 PLACEHOLDERS[PLACEHOLDER_COUNT] = PLACEHOLDER_COLORS;

vec4 withPlaceholders(vec4 base) {
    vec4 replaced = base;
    for (int i = 0; i < PLACEHOLDER_COUNT; i += 1) {
        if (colorEq(base.rgb, PLACEHOLDERS[i])) {
            replaced.rgb = uPlaceholderColors[i];
        }
    }
    return replaced;
}

#else

vec4 withPlaceholders(vec4 base) {
    return base;
}

#endif