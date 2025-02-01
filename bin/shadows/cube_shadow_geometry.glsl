#version 400 core

layout (triangles) in;
layout (triangle_strip, max_vertices = 18) out;

uniform mat4 shadowMatrices[6]; // Array of view-projection matrices for the 6 faces

in vec4 FragPos[];  // from vertex shader
// (Optionally pass additional data if needed)
out vec4 gs_FragPos;

void main() {
    // Loop over each face of the cube map.
    for (int face = 0; face < 6; face++) {
        gl_Layer = face;  // Tell OpenGL to render to the 'face'-th layer
        for (int i = 0; i < 3; i++) {
            gs_FragPos = FragPos[i];
            gl_Position = shadowMatrices[face] * FragPos[i];
            EmitVertex();
        }
        EndPrimitive();
    }
}
