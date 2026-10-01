uniform sampler2D Sampler0;
uniform float GlintTime;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

const float PI = 3.14159265359;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float glyphs(vec2 p, float t, float strength) {
    float column = floor(p.x);
    float speed = 0.6 + 0.8 * hash(vec2(column, 3.7));
    float y = p.y - t * 0.04 * speed + hash(vec2(column, 9.1)) * 40.0;
    vec2 cell = vec2(column, floor(y));
    if (hash(cell) < mix(0.97, 0.3, strength * strength)) {
        return 0.0;
    }
    vec2 local = (vec2(fract(p.x), fract(y)) - 0.5) * 1.25 + 0.5;
    if (any(lessThan(local, vec2(0.0))) || any(greaterThan(local, vec2(1.0)))) {
        return 0.0;
    }
    float code = 97.0 + floor(hash(cell + vec2(5.3, 1.9)) * 26.0);
    vec2 atlas = (vec2(mod(code, 16.0), floor(code / 16.0)) + vec2(local.x, 1.0 - local.y)) / 16.0;
    float fade = sin(fract(t * 0.015 * speed + hash(cell + vec2(2.3, 7.1))) * PI);
    return texture(Sampler0, atlas).a * fade;
}

void main() {
    float strength = vertexColor.a;
    float t = GlintTime;
    vec2 p = texCoord0;

    float haze = pow(noise(p * 1.4 + vec2(t * 0.01, -t * 0.05)), 2.0);
    float waves = 0.5 + 0.5 * sin(p.y * 3.0 + p.x * 1.2 - t * 0.15);
    float pulse = 0.85 + 0.15 * sin(t * 0.13);
    float glow = mix(0.2, 1.3, strength * strength) * (0.4 + 0.6 * haze) * (0.65 + 0.35 * waves) * pulse;
    float runes = glyphs(p, t, strength) * mix(0.6, 1.5, strength);

    float light = glow + runes;
    if (light < 0.004) {
        discard;
    }
    vec3 color = mix(vertexColor.rgb, vec3(1.0), clamp(0.5 * runes + 0.25 * strength * glow, 0.0, 1.0));
    fragColor = vec4(color, min(light, 1.0));
}
