
in vec2 fTexCoords;

uniform vec4 uColor;

out vec4 oColor;

void main() {
    oColor = uColor;
    oColor.rgb *= uColor.a;
}