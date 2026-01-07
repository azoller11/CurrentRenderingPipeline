#version 330 core

layout (location = 0) in vec3 position;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

out vec3 vWorldPos;

void main() {
    vec4 world = model * vec4(position, 1.0);
    vWorldPos = world.xyz;
    gl_Position = projection * view * world;
}
