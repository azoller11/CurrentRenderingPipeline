#version 400 core
layout (triangles) in;
layout (triangle_strip, max_vertices = 18) out;

uniform mat4 shadowMatrices[6];

in vec4 FragPos[];
out vec4 FragPosWorld;

void main() {
    for (int face = 0; face < 6; face++) {
        gl_Layer = face; // Render to correct face of the cubemap
        for (int i = 0; i < 3; i++) {
            FragPosWorld = FragPos[i];
            gl_Position = shadowMatrices[face] * FragPos[i];
            EmitVertex();
        }
        EndPrimitive();
    }
}
