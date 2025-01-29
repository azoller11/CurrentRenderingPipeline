#version 400 core
layout(triangles) in;
layout(triangle_strip, max_vertices=3) out;

in TES_OUT {
    vec2 uv;
    vec3 wPosition;
    vec3 wNormal;
    vec3 wTangent;
} gs_in[];

out GS_OUT {
    vec2 uv;
    vec3 wPosition;
    vec3 wNormal;
    vec3 wTangent;
} gs_out;

void main() {
    for(int i=0; i<3; i++) {
        gs_out.uv        = gs_in[i].uv;
        gs_out.wPosition = gs_in[i].wPosition;
        gs_out.wNormal   = gs_in[i].wNormal;
        gs_out.wTangent  = gs_in[i].wTangent;
        gl_Position      = gl_in[i].gl_Position;
        EmitVertex();
    }
    EndPrimitive();
}
