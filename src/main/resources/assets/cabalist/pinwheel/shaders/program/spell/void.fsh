uniform float VoidOpacity;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    if (vertexColor.a < 0.004) {
        discard;
    }
    fragColor = vec4(0.0, 0.0, 0.0, VoidOpacity);
}
