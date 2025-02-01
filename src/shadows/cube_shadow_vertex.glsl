#version 400 core

layout(location = 0) in vec3 inPosition;

uniform mat4 model;

out vec4 FragPos;

void main() {
    // Compute the world-space position of the vertex.
    FragPos = model * vec4(inPosition, 1.0);
}
