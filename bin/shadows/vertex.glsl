#version 400 core

// Vertex attributes
layout (location = 0) in vec3 inPosition;
layout (location = 1) in vec2 inTexCoord;  // New: texture coordinates

// Uniforms
uniform mat4 model;
uniform mat4 lightSpaceMatrix;

// Pass texture coordinates to the fragment shader
out vec2 passTexCoord;

void main() {
    gl_Position = lightSpaceMatrix * model * vec4(inPosition, 1.0);
    passTexCoord = inTexCoord;
}