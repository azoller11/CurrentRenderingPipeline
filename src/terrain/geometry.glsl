#version 400 core

layout(triangles) in;
layout(triangle_strip, max_vertices = 3) out;

in VS_OUT {
    vec2 uv;
    vec2 baseUV;
    vec3 worldPos;
    vec3 normal;
    vec3 tangent;
    vec4 lightSpacePos;
} gs_in[];

out VS_OUT {
    vec2 uv;
    vec2 baseUV;
    vec3 worldPos;
    vec3 normal;
    vec3 tangent;
    vec4 lightSpacePos;
} gs_out;

void main()
{
    for (int i = 0; i < 3; i++)
    {
        gs_out.uv            = gs_in[i].uv;
        gs_out.baseUV        = gs_in[i].baseUV;
        gs_out.worldPos      = gs_in[i].worldPos;
        gs_out.normal        = gs_in[i].normal;
        gs_out.tangent       = gs_in[i].tangent;
        gs_out.lightSpacePos = gs_in[i].lightSpacePos;

        gl_Position = gl_in[i].gl_Position;
        EmitVertex();
    }
    EndPrimitive();
}
