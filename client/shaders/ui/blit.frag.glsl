
in vec2 fTexCoords;

uniform sampler2D uTexture;

out vec4 oColor;

void main() {
    oColor = texture(uTexture, fTexCoords);
    #ifdef PREMULTIPLY_ALPHA
        oColor *= oColor.a;
    #endif
}