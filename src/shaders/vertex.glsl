#version 400 core

layout(location = 0) in vec3 inPosition;     // Vertex position
layout(location = 1) in vec2 inTexCoord;     // Texture coordinates
layout(location = 2) in vec3 inNormal;       // Vertex normal (averaged per vertex)
layout(location = 3) in vec3 inTangent;      // Tangent vector
layout(location = 4) in vec3 inBitangent;    // Bitangent vector

// Output structure passed to the next stage (geometry or directly to fragment shader)
out VS_OUT {
    vec2 uv;
    vec3 wPosition;    // World-space position
    vec3 wNormal;      // World-space normal
    vec3 wTangent;     // World-space tangent
    vec3 wBitangent;   // World-space bitangent
} vs_out;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main() {
    // Transform the vertex position to world space.
    vec4 worldPos = model * vec4(inPosition, 1.0);
    vs_out.wPosition = worldPos.xyz;

    // Compute the proper normal transformation in case of non-uniform scaling.
    mat3 normalMatrix = transpose(inverse(mat3(model)));
    
    vs_out.wNormal  = normalize(normalMatrix * inNormal);
    vs_out.wTangent = normalize(normalMatrix * inTangent);
    
    // Transform the bitangent to world space.
    vs_out.wBitangent = normalize(normalMatrix * inBitangent);

    // Pass along texture coordinates.
    vs_out.uv = inTexCoord;

    // Compute the final clip space position.
    gl_Position = projection * view * worldPos;
}
