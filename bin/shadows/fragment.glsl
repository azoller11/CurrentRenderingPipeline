#version 330 core

in vec2 passTexCoord;

uniform sampler2D diffuseMap;
uniform float alphaThreshold;
uniform bool useTexture;

void main() {
    if (useTexture) {
        float a = texture(diffuseMap, passTexCoord).a;
        if (a < alphaThreshold) discard;
    }
    // No color output. Depth is written automatically.
}
