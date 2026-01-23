
in vec2 fTexCoords;

uniform sampler2D uTexture;
uniform vec2 uDestSizePx;
uniform float uBorderRadius;

out vec4 oColor;

/*             X=0
 * +----+---------------+----+
 * | #1 | #2    :       | #1 |
 * +----+---------------+----+
 * | #3 | #4    :       | #3 |
 * |....|.......@.......|....| Y=0
 * |    |       :       |    |
 * +----+---------------+----+
 * | #1 | #2    :       | #1 |
 * +----+---------------+----+
 *
 * dCenter - distance from point to center (@)
 * insideDCenter - distance on each axis from center (@) to be inside of inner
 *                 square (#4) ((total size / 2) - radius)
 *
 * #1 - dCenter   > insideDCenter (both axes)
 * #2 - dCenter.y > insideDCenter.y
 * #3 - dCenter.x > insideDCenter.x
 * #4 - dCenter   < insideDCenter
 *
 * The border radius is equal to:
 * - The width and height of each square #1
 * - The height of each rectangle #2
 * - The width of each rectangle #3
 * Inside of each corner square (#1) the distance to the respective corner of
 * the inner square (#4) must be less than the border radius to be visible.
 */

void main() {
    float radius = uBorderRadius;
    vec2 fPosRelUv = fTexCoords - 0.5; // -0.5..+0.5, origin = center
    vec2 dCenter = abs(fPosRelUv * uDestSizePx);
    vec2 insideDCenter = (uDestSizePx / 2.0) - radius;
    bool fullyInside = dCenter.x <= insideDCenter.x
        || dCenter.y <= insideDCenter.y;
    if (fullyInside) {
        oColor = texture(uTexture, fTexCoords);
    } else {
        float bDist = length(dCenter - insideDCenter) - radius;
        float alpha = 1.0 - smoothstep(0.0, 1.0, bDist);
        oColor = texture(uTexture, fTexCoords) * alpha;
    }
}