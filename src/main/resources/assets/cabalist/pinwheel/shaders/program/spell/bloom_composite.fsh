uniform sampler2D DiffuseSampler0;
uniform sampler2D SpellBloom;
uniform sampler2D SpellBloomBlur;

in vec2 texCoord;

out vec4 fragColor;

const float BLUR_STRENGTH = 1.2;
const float CORE_STRENGTH = 0.4;

void main() {
    vec4 base = texture(DiffuseSampler0, texCoord);
    vec3 glow = texture(SpellBloomBlur, texCoord).rgb * BLUR_STRENGTH + texture(SpellBloom, texCoord).rgb * CORE_STRENGTH;
    fragColor = vec4(base.rgb + glow, base.a);
}
