#version 400 core

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec2 inTexCoord;
layout(location = 2) in float inBlend;  // Blend factor from TerrainGenerator

out vec2 vTexCoord;
out float vBlend;

void main() {
    vTexCoord = inTexCoord;
    vBlend = inBlend;
    gl_Position = vec4(inPosition, 1.0);
}
