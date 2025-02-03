#version 400 core
layout(triangles, equal_spacing, cw) in;

in TCS_OUT {
    vec2 uv;
    vec3 wPosition;
    vec3 wNormal;
    vec3 wTangent;
} tes_in[];

out TES_OUT {
    vec2 uv;
    vec3 wPosition;
    vec3 wNormal;
    vec3 wTangent;
} tes_out;

void main()
{
    vec3 b = gl_TessCoord.xyz;

    // Interpolate uv, wPosition, wNormal, wTangent
    tes_out.uv =
        b.x * tes_in[0].uv +
        b.y * tes_in[1].uv +
        b.z * tes_in[2].uv;

    tes_out.wPosition =
        b.x * tes_in[0].wPosition +
        b.y * tes_in[1].wPosition +
        b.z * tes_in[2].wPosition;

    tes_out.wNormal =
        b.x * tes_in[0].wNormal +
        b.y * tes_in[1].wNormal +
        b.z * tes_in[2].wNormal;
        
        

    tes_out.wTangent =
        b.x * tes_in[0].wTangent +
        b.y * tes_in[1].wTangent +
        b.z * tes_in[2].wTangent;

    gl_Position =
        b.x * gl_in[0].gl_Position +
        b.y * gl_in[1].gl_Position +
        b.z * gl_in[2].gl_Position;
}
