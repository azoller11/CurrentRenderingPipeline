#version 400 core

layout(location = 0) in vec3 inPosition;  // 
layout(location = 1) in vec2 inTexCoord;  
layout(location = 2) in vec3 inNormal;
layout(location = 3) in vec3 inTangent;   // new tangent attribute

out VS_OUT {
    vec2 uv;
    vec3 wPosition;
    vec3 wNormal;
    vec3 wTangent; // pass along
} vs_out;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main() {
    vec4 worldPos = model * vec4(inPosition, 1.0);
    vs_out.wPosition = worldPos.xyz;

    // Transform normal & tangent if there's scaling or rotation
    // For uniform scaling, mat3(model) works. 
    // For non-uniform, you need a proper normal matrix (inverse-transpose).
    vs_out.wNormal   = mat3(model) * inNormal;
    vs_out.wTangent  = mat3(model) * inTangent;

    vs_out.uv = inTexCoord;
    gl_Position = projection * view * worldPos;
}
