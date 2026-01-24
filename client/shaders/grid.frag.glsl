
in vec3 fPosWorld;

uniform float uGridResolution;
uniform float uLineRadius;
uniform vec4 uGridColor;

out vec4 oColor;

void main(void) {
    vec2 posGrid = fPosWorld.xz / uGridResolution;
    vec2 posRounded = round(posGrid);
    vec2 lineDist = abs(posGrid - posRounded);
    float minLineDist = min(lineDist.x, lineDist.y);
    float aa = fwidth(minLineDist);
    float alpha = 1.0 - smoothstep(
        uLineRadius - aa,
        uLineRadius + aa,
        minLineDist
    );
    if (alpha <= 0.0) { discard; }
    oColor = uGridColor * alpha;
    oColor.rgb /= uGridColor.a;
}
