#version 400 core

layout(vertices = 4) out;

uniform vec3 cameraPos;

// Tessellation multipliers
uniform float tessMin = 2.0;
uniform float tessMax = 16.0;
uniform float tessDistance = 400.0;

in VS_OUT {
    vec2 uv;
    vec2 baseUV;
    vec3 worldPos;
    vec3 normal;
    vec3 tangent;
    vec4 lightSpacePos;
} tcs_in[];

out VS_OUT {
    vec2 uv;
    vec2 baseUV;
    vec3 worldPos;
    vec3 normal;
    vec3 tangent;
    vec4 lightSpacePos;
} tcs_out[];

void main()
{
    // Pass through data
    tcs_out[gl_InvocationID].uv            = tcs_in[gl_InvocationID].uv;
    tcs_out[gl_InvocationID].baseUV        = tcs_in[gl_InvocationID].baseUV;
    tcs_out[gl_InvocationID].worldPos      = tcs_in[gl_InvocationID].worldPos;
    tcs_out[gl_InvocationID].normal        = tcs_in[gl_InvocationID].normal;
    tcs_out[gl_InvocationID].tangent       = tcs_in[gl_InvocationID].tangent;
    tcs_out[gl_InvocationID].lightSpacePos = tcs_in[gl_InvocationID].lightSpacePos;

    // Compute tessFactor based on distance to camera
    float dist = length(tcs_in[gl_InvocationID].worldPos - cameraPos);
    float factor = mix(tessMax, tessMin, clamp(dist / tessDistance, 0.0, 1.0));

    // Set tessellation levels only for invocation 0
    if (gl_InvocationID == 0)
    {
        gl_TessLevelOuter[0] = factor;
        gl_TessLevelOuter[1] = factor;
        gl_TessLevelOuter[2] = factor;
        gl_TessLevelOuter[3] = factor;

        gl_TessLevelInner[0] = factor;
        gl_TessLevelInner[1] = factor;
    }
}
