#version 400 core
layout(location = 0) in vec3 position;
out vec3 texCoords;

uniform mat4 view;
uniform mat4 projection;

void main() {
    // Use only the rotational part of the view matrix (translation removed) for the skybox.
    texCoords = position; // or a normalized direction from the cube/sphere vertex
    gl_Position = projection * view * vec4(position, 1.0);
}
