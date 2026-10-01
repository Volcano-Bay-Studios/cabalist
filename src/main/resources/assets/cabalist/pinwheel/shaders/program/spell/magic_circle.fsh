#define TAU 6.28318530718

uniform float CircleTime;

in vec4 vertexColor;
in vec2 localPosition;
flat in ivec2 circleData;
flat in float drawIn;

out vec4 fragColor;

float segmentDistance(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

bool revealed(float angle, float start, float drawIn) {
    return fract((angle - start) / TAU) <= drawIn;
}

bool inBand(float r, float outer, float width) {
    return r >= outer - width && r < outer;
}

void main() {
    float outer = float(circleData.x);
    float seed = float(circleData.y & 511);
    int runeRings = (circleData.y >> 9) & 7;
    int points = 3 + ((circleData.y >> 12) & 7);
    float fade = vertexColor.a;
    float t = CircleTime;

    vec2 p = floor(localPosition * 16.0) + 0.5;
    float r = length(p);
    float a = atan(p.y, p.x);
    bool lit = false;

    if (inBand(r, outer, 2.0) && revealed(a, 0.0, drawIn)) {
        float notch = fract((a - t * 0.3 / outer) / TAU * 8.0) * TAU * outer / 8.0;
        lit = notch >= 2.0;
    }

    float bandInner = outer - 2.0 - 11.0 * float(runeRings);
    for (int ring = 1; ring <= 7; ring++) {
        if (ring > runeRings) {
            break;
        }
        if (inBand(r, outer - 2.0 - 11.0 * float(ring), 1.0) && revealed(a, 3.14159 * float(ring), drawIn)) {
            lit = true;
        }
    }

    float tickOuter = bandInner - 2.0;
    float tickCount = max(8.0, floor(TAU * tickOuter / 4.0));
    float tick = (a + t * 0.2 / max(tickOuter, 1.0)) / TAU * tickCount;
    float tickLength = mod(floor(tick), 4.0) < 0.5 ? 4.0 : 2.0;
    if (inBand(r, tickOuter, tickLength) && fract(tick) * TAU * tickOuter / tickCount < 1.0 && revealed(a, 0.0, drawIn)) {
        lit = true;
    }

    float starRadius = tickOuter - 7.0;
    if (starRadius >= 6.0) {
        if (inBand(r, starRadius, 1.0) && revealed(a, 1.5708, drawIn)) {
            lit = true;
        }
        int skip = (points - 1) / 2;
        float turn = -t * 0.1 / starRadius + seed;
        for (int i = 0; i < 8; i++) {
            if (i >= points || float(i) >= drawIn * float(points)) {
                break;
            }
            float from = turn + TAU * float(i) / float(points);
            float to = turn + TAU * float((i + skip) % points) / float(points);
            vec2 corner = starRadius * vec2(cos(from), sin(from));
            vec2 other = starRadius * vec2(cos(to), sin(to));
            if (segmentDistance(p, corner, other) < 0.6 || abs(length(p - corner) - 2.0) < 0.6) {
                lit = true;
            }
        }

        float core = floor(starRadius * 0.4);
        if (core >= 3.0) {
            float dashes = max(4.0, floor(TAU * core / 3.0));
            if (inBand(r, core, 1.0) && fract((a - t * 0.3 / core) / TAU * dashes) < 0.66 && revealed(a, 0.0, drawIn)) {
                lit = true;
            }
        }
    }

    if (r < 1.5) {
        lit = true;
    }
    if (!lit) {
        discard;
    }
    float shimmer = 0.7 + 0.3 * sin(a * 3.0 - t * 0.15);
    fragColor = vec4(vertexColor.rgb, fade * shimmer);
}