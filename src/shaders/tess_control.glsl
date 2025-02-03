#version 400 core
layout(vertices = 3) out;

in VS_OUT {
    vec2 uv;
    vec3 wPosition;
    vec3 wNormal;
    vec3 wTangent;
    vec3 wBitangent;
} tcs_in[];

out TCS_OUT {
    vec2 uv;
    vec3 wPosition;
    vec3 wNormal;
    vec3 wTangent;
    vec3 wBitangent;
} tcs_out[];

void main() {
    gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;

    tcs_out[gl_InvocationID].uv        = tcs_in[gl_InvocationID].uv;
    tcs_out[gl_InvocationID].wPosition = tcs_in[gl_InvocationID].wPosition;
    tcs_out[gl_InvocationID].wNormal   = tcs_in[gl_InvocationID].wNormal;
    tcs_out[gl_InvocationID].wTangent  = tcs_in[gl_InvocationID].wTangent;
    
   

    // minimal tess
    gl_TessLevelInner[0] = 1.0;
    gl_TessLevelOuter[0] = 1.0;
    gl_TessLevelOuter[1] = 1.0;
    gl_TessLevelOuter[2] = 1.0;
}
