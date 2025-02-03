#version 400 core

layout(location = 0) in vec3 inPosition;  // Vertex position
layout(location = 1) in vec2 inTexCoord;   // Texture coordinates
layout(location = 2) in vec3 inNormal;     // Vertex normal (should be averaged per vertex)
layout(location = 3) in vec3 inTangent;      // Tangent vector

out VS_OUT {
    vec2 uv;
    vec3 wPosition;
    vec3 wNormal;
    vec3 wTangent;
} vs_out;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main() {
    vec4 worldPos = model * vec4(inPosition, 1.0);
    vs_out.wPosition = worldPos.xyz;

    // Use the proper normal matrix if non-uniform scaling is applied.
    // If you're sure the model matrix uses only uniform scaling, you can use mat3(model).
    mat3 normalMatrix = transpose(inverse(mat3(model)));
    vs_out.wNormal  = normalize(normalMatrix * inNormal);
    vs_out.wTangent = normalize(normalMatrix * inTangent);

    vs_out.uv = inTexCoord;
    gl_Position = projection * view * worldPos;
}
