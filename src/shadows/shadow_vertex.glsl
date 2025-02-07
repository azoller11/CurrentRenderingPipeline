#version 330 core

// Vertex position attribute (location 0)
layout (location = 0) in vec3 aPos;

// Uniform matrices
uniform mat4 model;
uniform mat4 lightSpaceMatrix;

void main()
{
    // Transform the vertex position to light space
    gl_Position = lightSpaceMatrix * model * vec4(aPos, 1.0);
}
