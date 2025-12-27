
in vec2 fTexCoords;

uniform sampler2D uTexture;

out vec4 oColor;

void main() {
    vec4 texColor = texture(uTexture, fTexCoords);
    if (texColor.a == 0.0) { discard; }
    oColor = texColor;
}