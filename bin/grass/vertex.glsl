#version 400 core
layout(location = 0) in vec3 inPosition;

// Pass the position directly to the geometry shader.
out vec3 fragPos;

void main() {
    fragPos = inPosition;
    gl_Position = vec4(inPosition, 1.0);
}
