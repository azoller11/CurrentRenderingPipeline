#version 330 core

// Vertex attribute (location 0: vertex positions)
layout(location = 0) in vec3 aPos;

// Uniforms provided by the renderer
uniform mat4 model;
uniform mat4 shadowMatrix;

void main()
{
    // Transform the vertex position from model space to light's clip space.
    gl_Position = shadowMatrix * model * vec4(aPos, 1.0);
}
