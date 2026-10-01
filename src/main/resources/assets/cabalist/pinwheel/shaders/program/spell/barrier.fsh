uniform sampler2D Sampler0;
uniform float BarrierTime;
uniform vec2 ShellCells;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

const float TAU = 6.28318530718;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p, float period) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    vec2 a = vec2(mod(i.x, period), i.y);
    vec2 b = vec2(mod(i.x + 1.0, period), i.y);
    return mix(mix(hash(a), hash(b), f.x), mix(hash(a + vec2(0.0, 1.0)), hash(b + vec2(0.0, 1.0)), f.x), f.y);
}

float glyphs(vec2 p, float t) {
    float around = max(ShellCells.x, 1.0);
    float column = mod(floor(p.x), around);
    float speed = 0.6 + 0.8 * hash(vec2(column, 3.7));
    float y = p.y - t * 0.05 * speed + hash(vec2(column, 9.1)) * 40.0;
    vec2 cell = vec2(column, floor(y));
    vec2 local = vec2(fract(p.x), fract(y));
    float present = step(0.68, hash(cell));
    local = (local - 0.5) * 1.3 + 0.5;
    if (present < 0.5 || any(lessThan(local, vec2(0.0))) || any(greaterThan(local, vec2(1.0)))) {
        return 0.0;
    }
    float code = 97.0 + floor(hash(cell + vec2(5.3, 1.9)) * 26.0);
    vec2 atlas = (vec2(mod(code, 16.0), floor(code / 16.0)) + vec2(local.x, 1.0 - local.y)) / 16.0;
    float flicker = 0.7 + 0.3 * sin(t * 0.3 + hash(cell) * 30.0);
    return texture(Sampler0, atlas).a * flicker;
}

void main() {
    float t = BarrierTime;
    float height = clamp(texCoord0.y / max(ShellCells.y, 0.001), 0.0, 1.0);
    vec2 p = vec2(texCoord0.x + t * 0.03, texCoord0.y);
    float around = max(ShellCells.x, 1.0);
    float angle = p.x / around * TAU;
    float hazeCells = max(1.0, floor(around * 0.5));
    float shimmerWaves = max(1.0, floor(around * 0.6));

    float reach = 0.45 + 0.2 * sin(t * 0.04) + 0.08 * sin(t * 0.11 + angle * 2.0);
    float glow = exp(-height / max(reach, 0.05));

    float haze = pow(noise(vec2(p.x / around * hazeCells, height * 3.0 - t * 0.06), hazeCells), 3.0) * (1.0 - height * 0.5);
    float runes = glyphs(p, t) * (1.0 - height * 0.8);

    float shimmer = 0.85 + 0.15 * sin(t * 0.8 + angle * shimmerWaves + height * 11.0);
    float pulse = 0.8 + 0.2 * sin(t * 0.13);

    float light = (0.7 * glow + 0.35 * haze + 1.1 * runes + 0.04) * shimmer * pulse;
    float alpha = vertexColor.a * light;
    if (alpha < 0.004) {
        discard;
    }
    vec3 color = mix(vertexColor.rgb, vec3(1.0), clamp(0.4 * glow + 0.3 * runes, 0.0, 1.0));
    fragColor = vec4(color, alpha);
}
