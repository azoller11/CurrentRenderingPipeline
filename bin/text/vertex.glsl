#version 330 core

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 texCoords;

out vec2 TexCoords;

uniform mat4 projectionMatrix;

void main() {
    gl_Position = projectionMatrix * vec4(position, 0.0, 1.0);
    TexCoords = texCoords;
}