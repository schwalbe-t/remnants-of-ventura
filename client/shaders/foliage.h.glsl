
#pragma once

uniform float uBottomHeight;
uniform float uTopHeight;

float foliageHeightAt(float worldHeight) {
    float relHeight = worldHeight - uBottomHeight;
    float totalHeight = uTopHeight - uBottomHeight;
    return clamp(relHeight / totalHeight, 0.0, 1.0);
}
