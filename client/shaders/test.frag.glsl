
in vec3 fNormal;
in vec2 fTexCoords;

uniform sampler2D uTexture;

out vec4 oColor;

void main(void) {
    oColor = texture(uTexture, fTexCoords);
    if (oColor.a == 0.0) { discard; }
}